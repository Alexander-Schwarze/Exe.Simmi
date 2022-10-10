# Exe_Simmi
A Bot for Simmi that takes a Twitch Chat message and will post it on Discord

## Link to let the Bot join the Discord-Server
Click on this [Link](https://discord.com/oauth2/authorize?client_id=990734200766357546&scope=bot&permissions=8) to make the Bot join your Discord Server. Note that he will have Administrator rights.

## Setup
To setup the bot, you need to build the repository to an executable (jar, exe, ...).<br>

Before executing the program, you need a folder "data" on the same level as the executable with following files and their contents:
* twitchBotConfig.properties
    * channel=\<channel_name>
    * only_mods=\<true/false>
    * command_prefix=\<prefix for commands>
    * user_cooldown_seconds=\<cooldown per user per command> 
    * command_cooldown_seconds=\<cooldown per user per command>
    * leave_emote=\<twitch emote that appears when the bot leaves the chat>
    * arrive_emote=\<twitch emote that appears when the bot connects to chat>
    * confirm_emote=\<twitch emote that appears when the bot confirms a command>
    * reject_emote=\<twitch emote that appears when the bot denies a command>
    * explanation_emote=\<twitch emote that appears when the bot explains>
    * allowed_domains=\<a list of domains (without http or https, e.g. "clips.twitch.tv/") that are allowed for the SendClipCommand, separated by ",">
    * blacklisted_users=\<a list of blacklisted users, separated by ",">
    * blacklist_emote=\<twitch emote that appears when the bot messages a blacklisted user>
    * remind_command_users=\<a list of users that are allowed to use the remind-command, separated by ",">  
    * remind_emote=\<twitch emote that appears when the remind-message appears>
    * remind_emote_fail=\<twitch emote that appears when the user has not given a remind-message>
    * run_name_redeem_id=\<ID of the run name redeem on the twitch channel. If you don't have the ID ready, just use the title and the bot will display a warning with the ID, so you can replace it.>
    * amount_displayed_runner_names=\<Integer that shows how many runner names are shown on twitch chat via. runnerslist-command>
    * current_runner_name_pre_text=\<Pre text to display the current runner's name in your streaming software>
    * current_runner_name_post_text=\<Post text to display the current runner's name in your streaming software>
* discordBotConfig.properties
  * feedback_channel_id=\<discord channel id for the feedback channel>
  * game_channel_id=\<discord channel id for the game channel>
  * clip_channel_id=\<discord channel id for the clip channel>
  * embed_accent_color=\<color for the discord embed accent in HEX (with "#" at the beginning)>
* clipPlayer.properties
  * clip_location=\<path to the directory with the clips, you can use a relative path (e.g. ..\\..\\clips\\folder) or the direct path (e.g. D:\\Files\\Videos\\clips\\folder)>
  * allowed_video_files=\<a list of video file types (without the dot, e.g. "mp4") that are allowed for the ClipPlayer, seperated by ",">
  * port=\<the port the websocket will be working on. make sure to use an unusual port (e.g. 12345)>
* twitchtoken.txt
    * The only content: twitch bot account token
* discordtoken.txt
  * The only content: discord bot account token

Just replace the stuff in <> with the value described.

### Info for using the clip-player feature:
While the app is running, do not add/delete videos from the folder. If you want to change the clips, make sure to turn off the app and then turn on again as soon as you are done. Also do not adjust the playlist file. If you wish to reset it (though there is the button on the app for it), just delete it and start up the app again.

### Info for certain properties:
  * twitchBotConfig.properties - remind_command_users: Keep in mind that you need to explicitly name all users that are allowed to use that command. This includes the broadcaster and mods.
  * User lists: When you use a list of users (e.g. for the blacklist feature), you can write down their name in the properties file. Just keep in mind that they can easily change their name, which means it is better to use their IDs instead. For that the bot will issue a warning that contains the user's ID. Read the log-file to gather that information.
  * Paths: When you need to write down a path (e.g. for the property clipPlayer.properties - clip_location), make sure to use double backslashes (e.g. D:\\Files\\Videos\\).