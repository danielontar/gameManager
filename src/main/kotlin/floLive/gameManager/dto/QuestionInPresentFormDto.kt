package floLive.gameManager.dto


class QuestionInPresentFormDto (val category:String,
                                val difficulty:String ,
                                val question:String,
                                val questionNumber:Int,
                                val possibleAnswers:List<String>)