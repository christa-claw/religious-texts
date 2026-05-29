


python3 -c "
import webvtt, os, glob
for f in glob.glob('transcripts/**/*.vtt', recursive=True):
    txt = f.replace('.vtt', '.txt')
    with open(txt, 'w') as out:
        for caption in webvtt.read(f):
            out.write(caption.text + ' ')
"