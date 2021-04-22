# -*- conding: uft-8 -*-

import ntpath
import csv
import numpy as np
from os import listdir
import os
import csv

from music21 import converter
import pandas as pd
import tensorflow as tf
from keras.models import model_from_json
from keras_preprocessing import sequence
from keras import backend as K


def pitch_to_index(pitch):
    """Convert the scale to integer."""
    return {
        12 : 'C0', 13 : 'C#0', 14 : 'D0', 15 : 'D#0', 16 :'E0', 17 : 'F0', 18 : 'F#0',
        19 : 'G0', 20 : 'G#0', 21 : 'A0', 22 : 'A#0', 23 : 'B0',
        24 : 'C1', 25 : 'C#1', 26 : 'D1', 27 : 'D#1', 28 : 'E1', 29 : 'F1', 30 : 'F#1',
        31 :'G1', 32 : 'G#1', 33 : 'A1', 34 : 'A#1', 35 : 'B1',
        36 :'C2', 37 : 'C#2', 38 : 'D2', 38 :'D#2', 40 : 'E2', 41 : 'F2', 42 : 'F#2',
        43 :'G2', 44 :'G#2', 45 : 'A2', 46 : 'A#2', 47 : 'B2',
        48 :'C3', 49 : 'C#3', 50 : 'D3', 51 : 'D#3', 52 : 'E3', 53 : 'F3', 54 : 'F#3',
        55 : 'G3', 56 :'G#3', 57 : 'A3', 58 : 'A#3', 59 : 'B3',
        60 :'C4', 61 :'C#4', 62 : 'D4', 63 : 'D#4', 64 : 'E4', 65 : 'F4', 66 : 'F#4',
        67 :'G4', 68 : 'G#4', 69 : 'A4', 70 : 'A#4', 71 : 'B4',
        72 :'C5', 73 : 'C#5', 74 : 'D5', 75 : 'D#5', 76 :'E5', 77 : 'F5', 78 : 'F#5',
        79 : 'G5', 80 : 'G#5', 81 : 'A5', 82 : 'A#5', 83 : 'B5',
        84 : 'C6', 85 : 'C#6', 86 : 'D6', 87 : 'D#6', 88 :'E6', 89 : 'F6', 90 : 'F#6',
        91 : 'G6', 92 : 'G#6', 93 : 'A6', 94 :'A#6', 95 : 'B6',
        96 : 'C7', 97 : 'C#7', 97 : 'D7', 99 : 'D#7', 100 : 'E7', 101 : 'F7', 102 : 'F#7',
        103 : 'G7', 104 : 'G#7', 105 :'A7', 106 : 'A#7', 107 : 'B7',
        108 : 'C8', 109 : 'C#8', 110 : 'D8', 111 : 'D#8', 112 :'E8', 113: 'F8', 114 : 'F#8',
        115 : 'G8', 116 : 'G#8', 117 : 'A8', 118 : 'A#8', 119 : 'B8',
        120 : 'C9', 121 :'C#9', 122 : 'D9', 123 :'D#9', 124 : 'E9', 125 : 'F9', 126 : 'F#9',
        127 : 'G9'
    }.get(pitch, 'nan')

def csv_to_dataset(filename,writer):

    dir = "/opt/tomcat/server/webapps/ROOT/server/filestorage/"+writer+"/"

    file = dir + filename + '.csv'

    key_sig = []
    tempo = []
    pitch = []
    start_time = []
    chord_start_time = []

    f = open(file, 'r', encoding='utf-8')
    rdr = csv.reader(f)
    for line in rdr:
        if line[1] == 'key_sig' or line[2] == 'tempo' or line[3] == 'pitch' or line[5] == 'start_time':
            continue

        key_sig.append(line[1])
        tempo.append(line[2])
        pi = pitch_to_index(int(line[3]))
        if pi != 'nan' :
            k = float(line[5])
            start_time.append(int(k))
            if len(pi) == 3:
                pitch.append(pi[:2])
            elif len(pi) ==2:
                pitch.append(pi[:1])

    f.close()


    bar_count = 1
    basic_tempo = 60
    basic_duration = 480

    key = key_sig[1].split('/')

    if key[1] == '4':
        basic_duration = 480
    if key[1] == '8':
        basic_duration = 240

    tempo_int = tempo[1].split('.')
    note_tempo = int(tempo_int[0])
    standard_tempo = basic_tempo / note_tempo
    basic_duration = basic_duration * standard_tempo

    bar = int(key[0]) * basic_duration

    df = pd.DataFrame(columns=["bar", "note_name"])

    next_bar = bar

    for i in range(len(start_time)):

        if start_time[i] >= next_bar:
            while True:
                if start_time[i] >= next_bar:
                    bar_count = bar_count + 1
                    next_bar += bar
                else:
                    chord_start_time.append(start_time[i])
                    break

        new_df = pd.DataFrame([[bar_count, pitch[i]]],
                                  columns=["bar", "note_name"])

        df = df.append(new_df, ignore_index=True)

    df.to_csv(dir + filename + ".csv")  # depplearning_csv

    print("Done creating deep_csvs!")

    return chord_start_time

def chord_to_txt(filename, writer_id,chord_start_time, prediction_list):
    i = 0;

    with open("/opt/tomcat/server/webapps/ROOT/server/filestorage/"+writer_id+"/chord.txt", 'w') as lep:
        try:
            for chord in prediction_list:
                data = str(chord_start_time[i]) + " " + chord + "\r\n"
                lep.write(data)
                i += 1

            lep.close()
        except Exception as e:
            print("000000000")
            print(e)


def one_hot_encoding(length, one_index):
    """Return the one hot vector."""
    vectors = [0] * length
    vectors[one_index] = 1
    return vectors

def make_test_npys(file_name, song_sequence):
    """Create npy file for each song in the test set."""
    file_path = "dataset/test_npy"
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


def predict(filename):
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

    song = 'dataset/test_npy/'+filename+'.npy'


    note_sequence = sequence.pad_sequences(np.load(song), maxlen=32)

    # predict
    prediction_list = []
    net_output = model.predict(note_sequence)

    for chord_index in net_output.argmax(axis=1):
        prediction_list.append(chord_dictionary[chord_index])

    print(ntpath.basename(song), prediction_list) ## chrod result

    #chord_to_txt(chord_start_time, prediction_list)
    return prediction_list


def make_npy_file(writer_id):
    np.set_printoptions(threshold=np.inf)

    note_dictionary = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B']

    csv_path = "/opt/tomcat/server/webapps/ROOT/server/filestorage/"+writer_id+"/melody.csv"

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

    make_test_npys(ntpath.basename(csv_path), song_sequence)  # npy create

    np.save('dataset/test_vector.npy', np.array(result_input_matrix))

def generateModifyChord(filename,writer_id):
    tf.logging.set_verbosity(tf.logging.ERROR)
    prediction_list = []
    chord_start_time = csv_to_dataset(filename,writer_id)
    make_npy_file(writer_id)
    prediction_list = predict(filename)
    chord_to_txt(filename, writer_id,chord_start_time, prediction_list)
