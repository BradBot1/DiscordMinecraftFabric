A simple discord to minecraft link for fabric<br><br>
The commands are as follows:
* d!register \<sendingPerms\>
  * <b>sendingPerms</b> - a boolean to say if the channel should be able to send messages to the server, if absent defaults to false
* d!register \<serverID:channelID\>
  * <b>serverID:channelIDs</b> - two longs conjoined together by a ':', if absent the current channel is removed
* d!auth \<users...\>
  * <b>users...</b> - the mentions of the users you wish to give auth to
* d!unauth \<users...\>
  * <b>users...</b> - the mentions of the users you wish to remove auth from

On first load a config is made and libs are downloaded, after this you can open the config and input your bot token and add your [discord user id](https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-) to the 'auth' array, then once the bot starts go to your wanted link channel and run d!register true/false <br><br>
<b>If you find any issues or have any features you want added, please report them as a bug report and ill fix/add it asap</b><br>
<i>And yes, literally anything is on the table, i will add as complex as a feature as you need if i possible</i>
