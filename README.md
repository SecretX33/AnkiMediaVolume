# AnkiMediaVolume

A simple CLI tool that renames media files in Anki folders in a reversible way in a way that allows old programs, such as `MP3Gain`, to work with them.

Such programs do not support characters outside of ASCII, so this tool will rename any `mp3` file in Anki media folder that contains non-ascii characters to a temporary name, this way you can run the program without issues, and when you're done, rename them back to the original name using the undo option of this tool.

This tool is pretty simple, when it first runs, if you don't have a configuration file in the current directory, it'll create one and quit. You may need to edit it to set the correct paths. After that, you can just run the program again and use its very intuitive CLI options to perform the operations.