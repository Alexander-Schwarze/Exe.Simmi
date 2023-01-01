package redeems

import Redeem
import config.TwitchBotConfig
import logger

val runNameRedeem: Redeem = Redeem(
    id = TwitchBotConfig.runNameRedeemId,
    handler = {
        runNamesRedeemHandler.addRunName(redeemEvent.redemption.user.displayName)
    }
)