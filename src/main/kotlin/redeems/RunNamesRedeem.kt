package redeems

import Redeem
import config.TwitchBotConfig

val runNameRedeem: Redeem = Redeem(
    id = TwitchBotConfig.runNameRedeemId,
    handler = {
        runNamesRedeemHandler.addRunner(redeemEvent.redemption.user.displayName)
    }
)