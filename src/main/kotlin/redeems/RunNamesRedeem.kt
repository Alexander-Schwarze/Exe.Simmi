package redeems

import Redeem
import config.TwitchBotConfig
import logger

val runNameRedeem: Redeem = Redeem(
    id = TwitchBotConfig.runNameRedeemId,
    handler = {
        logger.info("Called runNameRedeem")
    }
)