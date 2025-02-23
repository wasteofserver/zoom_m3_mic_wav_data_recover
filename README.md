# Zoom M3 MicTrak file recovery

This is a data recovery carving tool for the Zoom M3 MicTrak audio recorder.

### When will this be used?

The most typical scenario would be that you have accidentally formated your SD card and then wrote
new data wiping out the Boot / FAT regions.

### How does it work?

This microphone stores both a `.wav` file and a raw waveform while recording. 

As the device does not know the final file length before recording ends, the engineering team
decided to store the wav file and the raw file interleaved.

As of today, all tested data recovery carving tools (photorec, Recuva, R-Studio, ReclaiMe) search 
for a valid `.wav` header, commonly known as a `RIFF` header, and then extract the file based on 
the data provided in the header. Unfortunately, this approach fails for Zoom M3 MicTrak files.

Why? Take a look at this mocked-up `RIFF` header:

```
header: riff
file_size: 1048 bytes
data_size: 1024 bytes
data: (then 1024 bytes of audio data)
```

However, as mentioned earlier, both files are saved in an interleaved manner. 
This means that the data chunk will contain the combined data for both the `.wav` and the raw file.

```
data: raw1|wav1|raw2|wav2|...|rawN|wavN
```

So when your recovery software finds a RIFF header, it will pull out the length of the file. 
Unfortunately, this means that only half of the actual file will be recovered (as actual data is
double because it has two files), that also results in a strange "echo" effect caused by you hearing 
the interleaved data.

This tool works by carving an image file from your device and searching for valid data segments. 
When it finds them, it reconstructs both the .wav and raw files into playable files that can also 
be read by the Zoom M3 Edit & Play software.

## Usage

Assuming you've already created an image file from your device, you can run the following command:

```bash
java -jar zoom-m3-mictrak-recovery.jar /path/to/card_image.img
```

## How to create an image file

Use `dd` or `ddrescue` to create an image file from your device.

On Windows you can use `Cygwin` to install `dd` or `ddrescue`.  
On MacOS and Linux you should already have `dd`.

```bash
ddrescue /dev/mySDcard /path/to/card_image.img /path/to/card_image.log
```

## Need help?

Feel free to drop a message! I'll be happy to help you try to recover your files.

## License

This project is licensed under the MIT License, which means you are free to use, 
modify, and distribute the code as long as you credit Frankie from wasteofserver.com as 
the original author.

If you decide to use this code, I'd love to hear about it — feel free to let me know!
