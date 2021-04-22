#-*- conding: uft-8 -*-

import ntpath
import csv
import numpy as np
from os import listdir
import os

import tensorflow as tf
from music21 import converter
import pandas as pd
from keras.models import model_from_json
from keras_preprocessing import sequence
from keras import backend as K

def pitch_to_index(pitch):
    """Convert the scale to integer."""
    return{
        'C0': 12,'C#0': 13,'D0': 14,'D#0': 15,'E0': 16,'F0': 17,'F#0': 18,
        'G0': 19,'G#0': 20,'A0': 21,'A#0': 22,'B0': 23,
        'C1': 24,'C#1': 25,'D1': 26,'D#1': 27,'E1': 28,'F1': 29,'F#1': 30,
        'G1': 31,'G#1': 32,'A1': 33,'A#1': 34,'B1': 35,
        'C2': 36,'C#2': 37,'D2': 38,'D#2': 39,'E2': 40,'F2': 41,'F#2': 42,
        'G2': 43,'G#2': 44,'A2': 45,'A#2': 46,'B2': 47,
        'C3': 48,'C#3': 49,'D3': 50,'D#3': 51,'E3': 52,'F3': 53,'F#3': 54,
        'G3': 55,'G#3': 56,'A3': 57,'A#3': 58,'B3': 59,
        'C4': 60,'C#4': 61,'D4': 62,'D#4': 63,'E4': 64,'F4': 65,'F#4': 66,
        'G4': 67,'G#4': 68,'A4': 69,'A#4': 70,'B4': 71,
        'C5': 72,'C#5': 73,'D5': 74,'D#5': 75,'E5': 76,'F5': 77,'F#5': 78,
        'G5': 79,'G#5': 80,'A5': 81,'A#5': 82,'B5': 83,
        'C6': 84,'C#6': 85,'D6': 86,'D#6': 87,'E6': 88,'F6': 89,'F#6': 90,
        'G6': 91,'G#6': 92,'A6': 93,'A#6': 94,'B6': 95,
        'C7': 96,'C#7': 97,'D7': 98,'D#7': 99,'E7': 100,'F7': 101,'F#7': 102,
        'G7': 103,'G#7': 104,'A7': 105,'A#7': 106,'B7': 107,
        'C8': 108,'C#8': 109,'D8': 110,'D#8': 111,'E8': 112,'F8': 113,'F#8': 114,
        'G8': 115,'G#8': 116,'A8': 117,'A#8': 118,'B8': 119,
        'C9': 120,'C#9': 121,'D9': 122,'D#9': 123,'E9': 124,'F9': 125,'F#9': 126,
        'G9': 127
        }.get(pitch,'nan')

def midi_to_csv(userDir,filename, key):
    file_dir_name = userDir
    
    #output_dir_name = userDir # deep test file
    #output_dir_csv_name = userDir # midi csv file
    
    file = filename+'.mid'

    curMidiFile = converter.parse(file_dir_name + '/' + file)
    
    key_sig = key.split('/')
    bar = 0
    bar_count= 1
    basic_tempo = 60
    basic_duration = 480
    chord_start_time = []

    note_tempo = curMidiFile.metronomeMarkBoundaries()[0][2].number

    if key_sig[1] == '4':
        basic_duration = 480
    if key_sig[1] == '8':
        basic_duration = 240

    standard_tempo = basic_tempo/note_tempo
    basic_duration = basic_duration * standard_tempo

    bar = int(key_sig[0]) * basic_duration

    df = pd.DataFrame(columns=["bar", "note_name"])
    df2 = pd.DataFrame(columns=["key_sig", "tempo", "pitch", "velocity", "start_time", "duration"])

    a = 0
    note_pitch =0
    volume = 0
    next_start_time = 0
    next_bar = bar
    rest_duration_sum = 0

    for part in curMidiFile.parts:
        for note in part:
            if getattr(note, 'isNote', None) and note.isNote:
                if a == 0:
                    next_start_time = float(round(note.offset, 3)) * basic_duration
                    a +=1
                
                notestr = note.pitch.name.strip('-');
                
                if next_start_time >= next_bar:
                    rest_sum_bar = next_bar + rest_duration_sum
                    if next_start_time >= rest_sum_bar:
                        bar_count = bar_count + 1
                        next_bar += bar
                        chord_start_time.append(next_start_time)
                        rest_duration_sum =0
                    
                note_pitch = note.pitch.midi
                volume = note.volume.velocity

                new_df = pd.DataFrame([[bar_count,  notestr]],
                                      columns=["bar", "note_name"])

                df = df.append(new_df, ignore_index=True)

            if getattr(note, 'isRest', None) and note.isRest:
                note_pitch = -1
                volume =0
                if a ==1:
                    rest_duration_sum = note.duration.quarterLength * basic_duration

            pulse_duration = note.duration.quarterLength * basic_duration
                
            #key -> key_sig
            new_df2 = pd.DataFrame([[key, note_tempo,  note_pitch, volume, next_start_time, pulse_duration]],
                                      columns=["key_sig", "tempo", "pitch", "velocity", "start_time", "duration"])

            df2 = df2.append(new_df2, ignore_index=True)

            next_start_time = next_start_time + pulse_duration # next start time
            
        df.to_csv(file_dir_name + "/" + file[:-4] + ".csv") # depplearning_csv
        
        df2.to_csv(file_dir_name + "/" + file[:-4] + "mid.csv") # midi_csv

    print("Done creating deep_csvs!")

    return chord_start_time
       

def chord_to_txt(userDir, filename, chord_start_time, prediction_list):
    i = 0;
    filepath = userDir+'/'+filename+'.txt'
    with open(filepath, 'w') as lep:
        try:
            for chord in prediction_list:
                data = str(chord_start_time[i]) + " " + chord + "\r\n"
                lep.write(data)
                i += 1

            lep.close()
        except Exception as e:
            print(e)


def one_hot_encoding(length, one_index):
    """Return the one hot vector."""
    vectors = [0] * length
    vectors[one_index] = 1
    return vectors

def make_test_npys(userDir, file_name, song_sequence):
    """Create npy file for each song in the test set."""
    file_path = userDir
    if not os.path.isdir(file_path):
        os.mkdir(file_path)
    np.save('%s/%s.npy' % (file_path, file_name.split('.')[0]), np.array(song_sequence))

def load_model():
    # load model file
    model_dir = 'model_json/'
    model_files = listdir(model_dir)
    for i, file in enumerate(model_files):
        print(str(i) + " : " + file)
    #file_number_model = int(input('Choose the model:'))
    model_file = model_files[0] # file_number_model
    model_path = '%s%s' % (model_dir, model_file)

    # load weights file
    weights_dir = 'model_weights/'
    weights_files = listdir(weights_dir)
    for i, file in enumerate(weights_files):
        print(str(i) + " : " + file)
    #file_number_weights = int(input('Choose the weights:'))
    weights_file = weights_files[0] #file_number_weights
    weights_path = '%s%s' % (weights_dir, weights_file)

    # load the model
    model = model_from_json(open(model_path).read())
    model.load_weights(weights_path)

    return model


def predict(userDir, filename):
    chord_dictionary = ['C:maj', 'C:min',
                        'C#:maj', 'C#:min',
                        'D:maj', 'D:min',
                        'D#:maj', 'D#:min',
                        'E:maj', 'E:min',
                        'F:maj', 'F:min',
                        'F#:maj', 'F#:min',
                        'G:maj', 'G:min',
                        'G#:maj', 'G#:min',
                        'A:maj', 'A:min',
                        'A#:maj', 'A#:min',
                        'B:maj', 'B:min']

    K.clear_session()
    model = load_model()
    model.compile(loss='categorical_crossentropy', optimizer='adam', metrics=['accuracy'])

    song = userDir+'/'+filename+'.npy'


    note_sequence = sequence.pad_sequences(np.load(song), maxlen=32)

    # predict
    prediction_list = []
    net_output = model.predict(note_sequence)

    for chord_index in net_output.argmax(axis=1):
        prediction_list.append(chord_dictionary[chord_index])

    print(ntpath.basename(song), prediction_list) ## chrod result

    #chord_to_txt(chord_start_time, prediction_list)
    return prediction_list


def make_npy_file(userDir, filename):
    np.set_printoptions(threshold=np.inf)

    note_dictionary = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B']

    csv_path = userDir+'/'+filename+'.csv'

    note_dict_len = len(note_dictionary)

    # list for final input/target vector
    result_input_matrix = []

    # make the matrix from csv data
    csv_ins = open(csv_path, 'r')
    next(csv_ins)  # skip first line
    reader = csv.reader(csv_ins)

    note_sequence = []
    song_sequence = []  # list for each song(each npy file) in the test set
    pre_measure = None

    for line in reader:
        measure = int(line[1])
        note = line[2]

        # find one hot index
        note_index = note_dictionary.index(note)

        one_hot_note_vec = one_hot_encoding(note_dict_len, note_index)

        if pre_measure is None:  # case : first line
            note_sequence.append(one_hot_note_vec)

        elif pre_measure == measure:  # case : same measure note
            note_sequence.append(one_hot_note_vec)

        else:  # case : next measure note
            song_sequence.append(note_sequence)
            result_input_matrix.append(note_sequence)
            note_sequence = [one_hot_note_vec]

        pre_measure = measure

    result_input_matrix.append(note_sequence)  # case : last measure note

    make_test_npys(userDir, ntpath.basename(csv_path), song_sequence)  # npy create

    np.save('dataset/test_vector.npy', np.array(result_input_matrix))

def generate(userDir,filename, key_sig):
    tf.logging.set_verbosity(tf.logging.ERROR)
    prediction_list = []
    chord_start_time = midi_to_csv(userDir,filename, key_sig)
    make_npy_file(userDir, filename)
    prediction_list = predict(userDir,filename)
    chord_to_txt(userDir, filename, chord_start_time, prediction_list)
