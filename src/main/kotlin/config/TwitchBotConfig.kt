package config

import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.seconds

object TwitchBotConfig {
    private val properties = Properties().apply {
        load(File("data/twitchBotConfig.properties").inputStream())
    }

    val channel: String = properties.getProperty("channel")
    val onlyMods = properties.getProperty("only_mods") == "true"
    val commandPrefix: String = properties.getProperty("command_prefix")
    val userCooldown = properties.getProperty("user_cooldown_seconds").toInt().seconds
    val commandCooldown = properties.getProperty("command_cooldown_seconds").toInt().seconds
    val leaveEmote: String = properties.getProperty("leave_emote")
    val arriveEmote: String = properties.getProperty("arrive_emote")
    val confirmEmote: String = properties.getProperty("confirm_emote")
    val rejectEmote: String = properties.getProperty("reject_emote")
    val explanationEmote: String = properties.getProperty("explanation_emote")
    val allowedDomains: List<String> = properties.getProperty("allowed_domains").split(",")
    val blacklistedUsers: List<String> = properties.getProperty("blacklisted_users").lowercase(Locale.getDefault()).split(",")
    val blacklistEmote: String = properties.getProperty("blacklist_emote")
    val remindCommandUsers: List<String> = properties.getProperty("remind_command_users").lowercase(Locale.getDefault()).split(",")
    val remindEmote: String = properties.getProperty("remind_emote")
    val remindEmoteFail: String = properties.getProperty("remind_emote_fail")
    val runNameRedeemId: String = properties.getProperty("run_name_redeem_id")
    val amountDisplayedRunnerNames = properties.getProperty("amount_displayed_runner_names").toInt()
    val currentRunnerNamePreText: String = properties.getProperty("current_runner_name_pre_text")
    val currentRunnerNamePostText: String = properties.getProperty("current_runner_name_post_text")
    val hitCounterLocation: String = properties.getProperty("hit_counter_location")
}