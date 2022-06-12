package floLive.gameManager.domain

class PlayerInGame(val userName:String, var gameId:String, var QuestionIndex:Int = 0, var currentScore:Int = 0){


    fun incQuestionIndex(){
        QuestionIndex++
    }

    fun resetQuestionIndex(){
        QuestionIndex=0
    }

    fun incScore(){
        currentScore++
    }

    fun resetScore(){
        currentScore = 0
    }

}