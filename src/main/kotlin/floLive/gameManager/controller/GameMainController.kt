package floLive.gameManager.controller

import floLive.gameManager.dto.AnswerResultDto
import floLive.gameManager.dto.PlayerAnswerDto
import floLive.gameManager.dto.PlayerParamsDto
import floLive.gameManager.dto.QuestionInPresentFormDto
import floLive.gameManager.service.GameMainService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("quizgame")
class GameMainController(val gameMainService: GameMainService) {

    //Download questions and start game by sending question
    @PostMapping("/start")
    fun getRestQuestions(@RequestBody playerParamsDto: PlayerParamsDto): QuestionInPresentFormDto {
        return gameMainService.createGame(playerParamsDto.userName,playerParamsDto.category,playerParamsDto.difficulty)
    }

    //receive answer
    @PostMapping("/answer")
    fun receiveAnswer(@RequestBody playerInGameAnswer: PlayerAnswerDto): AnswerResultDto? {
        return gameMainService.receiveAnswer(playerInGameAnswer)?:error("error2")
    }

    //get question
    @GetMapping("/question/{userName}")
    fun getQuestion(@PathVariable userName : String): QuestionInPresentFormDto? {
        return gameMainService.getQuestion(userName)
    }


    //get max score in a specific game
    @GetMapping("/max_score/{useName}")
    fun getMaxScore(@PathVariable useName : String):Map<String,Any>{
        return gameMainService.getMaxResultOfPlayerInGame(useName)
    }


}