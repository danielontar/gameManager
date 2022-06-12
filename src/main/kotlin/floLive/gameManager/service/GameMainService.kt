package floLive.gameManager.service

import floLive.gameManager.domain.PlayerInGame
import floLive.gameManager.dto.AnswerResultDto
import floLive.gameManager.dto.PlayerAnswerDto
import floLive.gameManager.dto.QuestionInPresentFormDto
import floLive.gameManager.dto.ResponseEntityDto
import floLive.gameManager.enums.Category
import floLive.gameManager.enums.Difficulty
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class GameMainService(private val restTemplate: RestTemplate) {

    private var playersInGameList = mutableListOf<PlayerInGame>()
    private var questionsInGameMap = mutableMapOf<String,List<QuestionInPresentFormDto>>()
    private var answersInGameList = mutableListOf<Int>()
    private var answersInGameMap = mutableMapOf<String,MutableList<Int>>() // gameId => List[2,3,1,0..]
    //Start
    fun createGame(userName:String, category: Category, difficulty: Difficulty):QuestionInPresentFormDto {
        var gameId = ""
        var playerInGame = playersInGameList.find { it.userName == userName }
        if (playerInGame==null){
            //Player is new in system.
            // Check for existing games running in system.
            if(playersInGameList.isNotEmpty()){
                //There are exiting games in system prior to his joining.
                gameId = playersInGameList[0].gameId
                playersInGameList.add(PlayerInGame(userName,gameId))
                PlayerInGame(userName,gameId).resetQuestionIndex()
                PlayerInGame(userName,gameId).resetScore()
                answersInGameList.clear()

            } else{
                // add new record of player in game
                gameId = UUID.randomUUID().toString()
                playersInGameList.add(PlayerInGame(userName,gameId))
            }
        } else{
            // The player has finished prior game.
            // update existing record set question index to 0 as player begun new game
            gameId = UUID.randomUUID().toString()
            playerInGame.gameId = gameId
            playerInGame.resetQuestionIndex()
            playerInGame.resetScore()
            answersInGameList.clear()
        }
        var categoryParam = 0
        var difficultyParam = ""
        categoryParam = when(category){
            Category.Sports -> 21
            Category.Geography -> 22
            Category.History -> 23
        }
        difficultyParam = when(difficulty){
            Difficulty.Easy -> "easy"
            Difficulty.Medium -> "medium"
            Difficulty.Hard -> "hard"

        }
        var amountParam = (1..2).random()
        val openTDBResourceUrl = "https://opentdb.com/api.php?amount=${amountParam}&category=${categoryParam}&difficulty=${difficultyParam}&type=multiple"
        val responseEntity: ResponseEntityDto? = restTemplate.getForObject(openTDBResourceUrl, ResponseEntityDto::class.java)
        val results = responseEntity?.results
        val questionOpenDBDtoList =  buildListOfQuestions(gameId,results)
        questionsInGameMap = mutableMapOf(gameId to questionOpenDBDtoList)
        return questionsInGameMap[gameId]?.get(0) ?: error("Question lists is empty.")
    }

    //End Start

    //Answer
    fun receiveAnswer(playerAnswer: PlayerAnswerDto): AnswerResultDto? {
        val playerInGame = searchPlayerInGame(playerAnswer.userName)
        val gameId = playerInGame?.gameId
        val questionIndex = playerInGame?.QuestionIndex
        var listSize = 0
        if (questionsInGameMap.isNotEmpty()){
            listSize = questionsInGameMap[gameId]!!.size
        }
        // check if game quiz is over, meaning no more questions
        if(listSize==null || questionIndex==listSize || questionIndex==null){
            return null
        }
        //else - Answer is correct
        val answerList =  answersInGameMap[gameId]
        if (answerList!!.isEmpty()){
            error("answerList is empty.")
        }
        if(playerAnswer.answerNumber == answerList?.get(questionIndex)){
            //Answer is incorrect
            searchPlayerInGame(playerAnswer.userName)?.incScore()
            searchPlayerInGame(playerAnswer.userName)?.incQuestionIndex()
            return AnswerResultDto(playerAnswer.userName,playerAnswer.answerNumber,"Answer is correct!")
        } else{
            //else - Answer is incorrect
            searchPlayerInGame(playerAnswer.userName)?.incQuestionIndex()
            return AnswerResultDto(playerAnswer.userName,playerAnswer.answerNumber,"Answer is incorrect. " +
                    "The correct answer number was ${answerList?.get(questionIndex)}.")
        }
    }

    //End Answer

    //Question
    fun getQuestion(userName:String): QuestionInPresentFormDto? {
        var questionList = listOf<QuestionInPresentFormDto>()
        var gameId:String = playersInGameList.find{it.userName==userName}?.gameId ?: error("No game was created for $userName")
        val questionIndex:Int = searchPlayerInGame(userName)!!.QuestionIndex
        if(questionsInGameMap.isNotEmpty()){
            questionList  = questionsInGameMap[gameId]!!
        }
        if(questionList.isNotEmpty()){
            if(questionIndex<questionList.size){
                return questionList[questionIndex]
            } else{
                val category:Category= listOf(Category.Sports, Category.Geography, Category.History).random()
                val difficulty = listOf(Difficulty.Easy,Difficulty.Medium,Difficulty.Hard).random()
                return createGame(userName,category,difficulty)
            }
        }
        return null
    }

    //End Question

    //Reports
    fun getMaxResultOfPlayerInGame(userName :String):Map<String,Any>{
        var userNameA= ""
        var maxScoreA = 0
        playersInGameList.forEach {
            if (it.userName==userName){
                if(it.currentScore>maxScoreA){
                    userNameA=it.userName
                    maxScoreA=it.currentScore
                }
            }
        }
        return mapOf("Leading scorer" to userNameA,"Max Scorer" to maxScoreA)
    }

    //End Reports

    //Help functions
    private fun buildListOfQuestions(gameId:String,results: List<Map<String,Any>>?): List<QuestionInPresentFormDto> {
        val questionInPresentFormDtoList: MutableList<QuestionInPresentFormDto> = mutableListOf()
        results?.forEachIndexed { index, it ->
            val category = it.getValue("category")
            val difficulty = it.getValue("difficulty")
            var question = it.getValue("question")
            question = replaceUndesiredChars(question as String)
            var correctAnswer = it.getValue("correct_answer")
            correctAnswer = replaceUndesiredChars(correctAnswer as String)
            val incorrectAnswers = it.getValue("incorrect_answers") as List<String>
            var possibleAnswers = mutableListOf<String>()
            incorrectAnswers.forEach() {
                possibleAnswers.add(replaceUndesiredChars(it))
            }
            possibleAnswers.add(correctAnswer)
            possibleAnswers.shuffle()
            val correctAnswerNumber = (possibleAnswers.indexOf(correctAnswer) + 1)
            answersInGameList.add(correctAnswerNumber)
            val questionInPresentFormDto = QuestionInPresentFormDto(category as String, difficulty as String, question, (index+1), possibleAnswers as List<String>)
            questionInPresentFormDtoList.add(questionInPresentFormDto)
        }
        answersInGameMap[gameId] = answersInGameList
        return questionInPresentFormDtoList
    }

    private fun searchPlayerInGame(userName: String): PlayerInGame?  {
        return playersInGameList.find{it.userName==userName} ?: error("Can't find game that ${{userName}} participates in.")
    }

    private fun replaceUndesiredChars(oldStr:String):String{
        return oldStr.replace("&quot;", "\"").replace("&#039","'").replace(";","")
    }
}