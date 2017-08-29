#!/usr/bin/python3

#required libraries
import ssl
import paho.mqtt.client as mqtt
import RPi.GPIO as GPIO
import time

GPIO.setmode(GPIO.BCM)
GPIO.setup(18, GPIO.IN, pull_up_down=GPIO.PUD_DOWN)

#ghpe0215
import subprocess
import os
import socket
import json
import urllib.request as urllib2
import base64
import sys
import time
import os
import json
from random import randint
counter = 0

movies = os.listdir("/home/pi/Desktop/sample_movies")
index = randint(0, len(movies)-1)
chosen_movie = "/home/pi/Desktop/sample_movies/" + movies[index]

config = {
    "kodi": {
        "ip": "127.0.0.1",
        "port": 8080,
        "username": "kodi",
        "password": "kodi",
        "init": [
                {
                "method": "Player.Open",
                "parameters": {
                    "item": {"file": chosen_movie}
                    }
                }
                ]
    }
}

yeelight_ip = "192.168.0.31"
yeelight_port = 55443
color = "White"
color_before = "White"
brightness = 5
brightness_before = 5
colors = {
    "Brown" :   0x8B4513,
    "Green" :   0x008000,
    "Red":      0xFF0000,
    "Blue":     0x0000FF,
    "Yellow":   0xFFFF00,
    "Cyan":     0x00FFFF,
    "Orange":   0xFFA500,
    "Pink":     0xFF69B4,
    "White":    0xFFFFFF,
    "Silver":   0xC0C0C0,
    "Purple":   0x800080,
}


def getJsonRemote(host,port,username,password,method,parameters):
    # First we build the URL we're going to talk to
    url = 'http://%s:%s/jsonrpc' %(host, port)
    # Next we'll build out the Data to be sent
    values ={}
    values["jsonrpc"] = "2.0"
    values["method"] = method
    if parameters:
        values["params"] = parameters
    values["id"] = "1"
    headers = {"Content-Type":"application/json",}
    # Format the data
    data = (json.dumps(values)).encode('utf-8')
    # Now we're just about ready to actually initiate the connection
    req = urllib2.Request(url)
    # This fork kicks in only if both a username & password are provided
    if username and password:
        # This properly formats the provided username & password and adds them to the request header
        base64string = base64.encodestring(('%s:%s' % (username,password)).encode()).decode().replace('\n', '')
        req.add_header("Authorization", "Basic %s" % base64string)
    # Now we're ready to talk to Kody
    # I wrapped this up in a try: statement to allow for graceful error handling
    try:
        req.add_header("Content-Type","application/json")
        response = urllib2.urlopen(req,data)
        response = response.read()
        response = json.loads(response.decode('utf-8'))
        # A lot of the Kodi responses include the value "result", which lets you know how your call went
        # This logic fork grabs the value of "result" if one is present, and then returns that.
        # Note, if no "result" is included in the response from Kodi, the JSON response is returned instead.
        # You can then print out the whole thing, or pull info you want for further processing or additional calls.
        if 'result' in response:
            response = response['result']
    # This error handling is specifically to catch HTTP errors and connection errors
    except urllib2.URLError as e:
        # In the event of an error, I am making the output begin with "ERROR " first, to allow for easy scripting.
        # You will get a couple different kinds of error messages in here, so I needed a consistent error condition to check for.
        response = 'ERROR '+str(e.reason)
    return response

def kodiRemoteControl(action):
    response = getJsonRemote(config["kodi"]["ip"], config["kodi"]["port"], config["kodi"]["username"], config["kodi"]["password"], action["method"], action["parameters"])
    print(response)
    return response

def kodiInit():
    movies = os.listdir("/home/pi/Desktop/sample_movies")
    index = randint(0, len(movies)-1)
    chosen_movie = "/home/pi/Desktop/sample_movies/" + movies[index]

    config = {
        "kodi": {
            "ip": "127.0.0.1",
            "port": 8080,
            "username": "kodi",
            "password": "kodi",
            "init": [
                    {
                    "method": "Player.Open",
                    "parameters": {
                        "item": {"file": chosen_movie}
                        }
                    }
                    ]
        }
    }
    for action in config["kodi"]["init"]:
        kodiRemoteControl(action)

def kodiPlayerStop():
    action = {
        "method": "Player.Stop",
            "parameters": {
                "playerid": 1
        }
        }
    kodiRemoteControl(action)
    yeelight_set_bright(brightness_before)
    yeelight_set_rgb(color_before)
    os.system("kodi-send --action=\"Quit\"")

def kodiPlayPause():
    action = {
        "method": "Player.PlayPause",
            "parameters": {
                "playerid": 1
        }
        }
    kodiRemoteControl(action)

def kodiPlayerGetProperty(playerId):
    action = {
        "method": "Player.GetProperties",
            "parameters": {
                "playerid": playerId,
                "properties" : ["speed","percentage","time","totaltime"]
            }
        }
    response = kodiRemoteControl(action)
    return response

def kodiIsActive():
    action = {
        "method": "Player.GetActivePlayers",
            "parameters": {
            }
        }
    response = kodiRemoteControl(action)
    for player in response:
        player_speed = kodiPlayerGetProperty(player["playerid"])["speed"]
        if player_speed != 0:
            print ("kode is active")
            return True
    return False

def kodiPlayerGetItemLabel():
    action = {
        "method": "Player.GetItem",
            "parameters": {
            "playerid": 1,
            "properties": ["title"]
            }
        }
    response = kodiRemoteControl(action)
    return response

def operate_on_bulb(method,params):
    command = "'{\"id\":1,\"method\":\"" + method + "\",\"params\":[" + params + "]}\\r\\n'"
    try:
        tcp_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        tcp_socket.connect((yeelight_ip,yeelight_port))
        msg="{\"id\":1,\"method\":\""
        msg = msg + method + "\",\"params\":[" + params + "]}\r\n"
        tcp_socket.send(msg.encode('utf-8'))
        answer = tcp_socket.recv(2048)
        answer = json.loads(answer.decode('utf-8'))
        print (answer)
        tcp_socket.close()
        return answer
    except Exception as e:
        print ("Unexpected error:", e)

def yeelight_toggle():
    operate_on_bulb("toggle","")

def yeelight_on():
    operate_on_bulb("set_power","\"on\"")

def yeelight_off():
    operate_on_bulb("set_power","\"off\"")

def yeelight_set_bright(brightness):
    operate_on_bulb("set_bright",str(brightness))

def yeelight_set_color(color): #string color
    operate_on_bulb("set_rgb",str(int(colors[color])))

def yeelight_set_rgb(rgb): # from 0 to 16777215 (hex: 0xFFFFFF)
    operate_on_bulb("set_rgb",str(rgb))

def yeelight_get_prop():
    ans = operate_on_bulb("get_prop","\"rgb\",\"bright\"")
    ans = ans['result']
    global color_before
    global brightness_before
    color_before = ans[0]
    brightness_before = ans[1]
    print (color_before,brightness_before)

#called while client tries to establish connection with the server
def on_connect(mqttc, obj, flags, rc):
    if rc==0:
        print ("Subscriber Connection status code: "+str(rc)+" | Connection status: successful")
    elif rc==1:
        print ("Subscriber Connection status code: "+str(rc)+" | Connection status: Connection refused")

#called when a topic is successfully subscribed to
def on_subscribe(mqttc, obj, mid, granted_qos):
    print("Subscribed: "+str(mid)+" "+str(granted_qos)+"data"+str(obj))

#called when a message is received by a topic
def on_message(mqttc, obj, msg):
    print("Received message from topic: "+msg.topic+" | QoS: "+str(msg.qos)+" | Data Received: "+str(msg.payload))
    if msg.topic == "$aws/things/RasPi3/shadow/update":
        tmp = ((msg.payload).decode("utf-8"))
        message = json.loads(tmp)
        change = message["state"]["change"]
        if change == "yeeLight":
            global color
            global brightness
            color = message["state"]["reported"]["color"]
            brightness = message["state"]["reported"]["brightness"]
            publish_yeeLight_settings_to_android(color,brightness)
            print (color,brightness)
            if brightness > 0:
                yeelight_on()
                yeelight_set_bright(brightness*10)
                yeelight_set_color(color)
            else :
                yeelight_off()
        if change == "movie":
            movie_mode = message["state"]["reported"]["movie_mode"]
            if movie_mode == "play":
                publish_play_to_android()
                kodiPlayPause()
            if movie_mode == "pause":
                publish_pause_to_android()
                kodiPlayPause()
            if movie_mode == "start":
                start_movie()
                #publish_play_to_android()
            if movie_mode == "stop":
                publish_to_android("stopped")
                kodiPlayerStop()
                
#creating a client with client-id=mqtt-test
mqttc = mqtt.Client("qasemSa",True,None,mqtt.MQTTv31)

mqttc.on_connect = on_connect
mqttc.on_subscribe = on_subscribe
mqttc.on_message = on_message

#Configure network encryption and authentication options. Enables SSL/TLS support.
#adding client-side certificates and enabling tlsv1.2 support as required by aws-iot service
mqttc.tls_set("/home/pi/Desktop/NewSDK/root-CA.crt",
	            certfile="/home/pi/Desktop/NewSDK/3db3a6dca1-certificate.pem.crt",
	            keyfile="/home/pi/Desktop/NewSDK/3db3a6dca1-private.pem.key",
              tls_version= ssl.PROTOCOL_TLSv1_2,
              ciphers=None)
print("aaaa")
#connecting to aws-account-specific-iot-endpoint
mqttc.connect("a18cbjggcfl7xb.iot.us-east-1.amazonaws.com", port=8883) #AWS IoT service hostname and portno
print("aaaa")
#the topic to publish to
#mqttc.subscribe("$aws/things/RasPi3/shadow/update", 1) #The names of these topics start with $aws/things/thingName/shadow."
mqttc.subscribe("my/topic", 1) #The names of these topics start with $aws/things/thingName/shadow."
mqttc.subscribe("$aws/things/RasPi3/shadow/update", qos=1)
print("aaaa")
import json
import urllib.request as urllibReq

ip = urllibReq.urlopen("http://ip.42.pl/raw").read().decode("utf-8")

payload = json.dumps({
    "state": {
        "reported": {
            "IP": ip
        }
    }
})
#mqttc.publish("$aws/things/RasPi3/shadow/update",payload)
#mqttc.publish("my/topic"," sss 1")
def publish_to_android(movie_mode):
    movie_name = kodiPlayerGetItemLabel()["item"]["label"]
    movie_prop = kodiPlayerGetProperty(1)
    movie_total_time = str(movie_prop["totaltime"]["hours"])+":"+str(movie_prop["totaltime"]["minutes"])+":"+str(movie_prop["totaltime"]["seconds"])
    current_movie_time = str(movie_prop["time"]["hours"])+":"+str(movie_prop["time"]["minutes"])+":"+str(movie_prop["time"]["seconds"])
    t = time.gmtime()
    t_time = str(t.tm_year)+":"+str(t.tm_mon)+":"+str(t.tm_mday)+":"+str(t.tm_hour)+":"+str(t.tm_min)+":"+str(t.tm_sec)
    message = json.dumps({
                         "IP": ip,
                         "movie_mode": movie_mode,
                         "movie_name": movie_name,
                         "movie_total_time": movie_total_time,
                         "color": color,
                         "brightness": brightness,
                         "yeeLight_sittings_changed": 0,
                         "current_movie_time": current_movie_time,
                         "gmt_time": t_time
                         })
    mqttc.publish("my/topic",message)

def publish_play_to_android():
    publish_to_android("playing")

def publish_pause_to_android():
    publish_to_android("paused")

def publish_yeeLight_settings_to_android(color,brightness):
    message = json.dumps({
                         "IP": ip,
                         "color": color,
                         "brightness": brightness,
                         "yeeLight_sittings_changed": 1,
                         })
    mqttc.publish("my/topic",message)

first_launch = True
is_movie_playing = False

def start_movie():
    global first_launch
    global is_movie_playing
    if "Connection refused" not in kodiPlayerGetItemLabel() :# if kodi is running
        print(kodiIsActive())
        if not kodiIsActive():
            #send init message
            kodiInit()
            publish_play_to_android()
            print('first launch kodi is running')
            is_movie_playing = True
            first_launch = False
    else :
        #launch kodi
        pid = os.fork()
        if pid == 0:
            os.system("kodi")
            os._exit(1)
            print("i'm a the son thread")
        else :
            time.sleep(5)
            kodiInit()
            publish_play_to_android()
            is_movie_playing = True
            first_launch = False
            
def buttonPressed(x):
    global first_launch
    global is_movie_playing
    global counter
    counter = counter +1
    print ("counter *************** " +str(counter))
    passed_something = False
    if first_launch and not passed_something :
        passed_something = True
        start_movie()
    else:
        if not is_movie_playing and not passed_something:
            #play movie
            passed_something = True
            kodiPlayPause()
            publish_play_to_android()
            print('play movie')
            is_movie_playing = True
            passed_something = True
        if is_movie_playing and not passed_something :
            #pause movie
            passed_something = True
            kodiPlayPause()
            publish_pause_to_android()
            print('pause movie')
            is_movie_playing = False

GPIO.add_event_detect(18, GPIO.FALLING, buttonPressed, bouncetime=2000)
yeelight_get_prop()
print (int(color_before))
print(int(brightness_before))
#automatically handles reconnecting+
mqttc.loop_forever()















        
