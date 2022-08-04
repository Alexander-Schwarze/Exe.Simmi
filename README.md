# Exe_Simmi
A Bot for Simmi that takes a Twitch Chat message and will post it on Discord

## Link to let the Bot join the Discord-Server
Click on this [Link](https://discord.com/oauth2/authorize?client_id=990734200766357546&scope=bot&permissions=8) to make the Bot join your Discord Server. Note that he will have Administrator rights.

## Setup
To setup the bot, you need to build the repository to an executable (jar, exe, ...).<br>

Before executing the program, you need a folder "data" on the same level as the executable with following content:
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
    * allowed_domains=\<a list of domains (without http or https, e.g. "clips.twitch.tv/") that are allowed for the SendClipCommand, seperated by ",">
    * blacklisted_users=\<a list of blacklisted users, separated by ",". You can use Twitch names or IDs, but IDs are better. The bot will log a warning containing the users ID so you can paste it in>
    * blacklist_emote=\<twitch emote that appears when the bot messages a blacklisted user>
* discordBotConfig.properties
  * feedback_channel_id=\<discord channel id for the feedback channel>
  * game_channel_id=\<discord channel id for the game channel>
  * clip_channel_id=\<discord channel id for the clip channel>
  * embed_accent_color=\<color for the discord embed accent in HEX (with "#" in front)>
* clipPlayer.properties
  * clip_location=\<path to the directory with the clips, you can use a relative path (e.g. ..\\..\\clips\\folder) or the direct path (e.g. D:\\Files\\Videos\\clips\\folder). Just make sure to write double back slashes>
  * allowed_video_files=\<a list of video file types (without the dot, e.g. "mp4") that are allowed for the ClipPlayer, seperated by ",">
  * port=\<the port the websocket will be working on. make sure to use an unusual port (e.g. 12345)>
* twitchtoken.txt
    * The only content: twitch bot account token
* discordtoken.txt
  * The only content: discord bot account token

Just replace the stuff in <> with the value described.

### Info for using the clip-player feature:
While the app is running, do not add/delete videos from the folder. If you want to change the clips, make sure to turn off the app and then turn on again as soon as you are done. Also do not adjust the playlist file. If you wish to reset it (though there is the button on the app for it), just delete it and start up the app again.