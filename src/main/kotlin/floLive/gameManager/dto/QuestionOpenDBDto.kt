package floLive.gameManager.dto


class QuestionOpenDBDto(val category: String,
                        val difficulty: String,
                        val question: String,
                        val correctAnswer: String,
                        val incorrectAnswers:List<String>)