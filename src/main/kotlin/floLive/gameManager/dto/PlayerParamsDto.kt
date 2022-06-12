package floLive.gameManager.dto

import floLive.gameManager.enums.Category
import floLive.gameManager.enums.Difficulty

class PlayerParamsDto (val userName:String,
                       val category: Category,
                       val difficulty: Difficulty
)