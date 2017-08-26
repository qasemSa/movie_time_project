#!/usr/bin/python3

#required libraries
import ssl
import paho.mqtt.client as mqtt
#import RPi.GPIO as GPIO
import time

#GPIO.setmode(GPIO.BCM)
#GPIO.setup(18, GPIO.IN, pull_up_down=GPIO.PUD_UP)

#!/usr/bin/python
#ghpe0215
import subprocess
import os
import socket
import json
import urllib2
import base64
import sys
import time
import os
import json

config = {}
yeelight_ip = "192.168.43.133"
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
    data = json.dumps(values)
    # Now we're just about ready to actually initiate the connection
    req = urllib2.Request(url, data, headers)
    # This fork kicks in only if both a username & password are provided
    if username and password:
        # This properly formats the provided username & password and adds them to the request header
        base64string = base64.encodestring('%s:%s' % (username, password)).replace('\n', '')
        req.add_header("Authorization", "Basic %s" % base64string)
    # Now we're ready to talk to Kody
    # I wrapped this up in a try: statement to allow for graceful error handling
    try:
        response = urllib2.urlopen(req)
        response = response.read()
        response = json.loads(response)
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
    print response
    return response

def kodiInit():
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
            print "kode is active"
            return False
    return True

def kodiPlayerGetItemLabel():
    action = {
        "method": "Player.GetItem",
            "parameters": {
            "playerid": 1,
            }
        }
    response = kodiRemoteControl(action)["item"]["label"]
    return response

movies = os.listdir("/Users/qasemsayah/Desktop/sample_movies")
index = randint(0, len(movies)-1)
chosen_movie = "/Users/qasemsayah/Desktop/sample_movies/" + movies[index]

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

def operate_on_bulb(method,params):
    command = "'{\"id\":1,\"method\":\"" + method + "\",\"params\":[" + params + "]}\\r\\n'"
    try:
        tcp_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        tcp_socket.connect((yeelight_ip,yeelight_port))
        msg="{\"id\":1,\"method\":\""
        msg += method + "\",\"params\":[" + params + "]}\r\n"
        tcp_socket.send(msg)
        answer = tcp_socket.recv()
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

def yeelight_set_rgb(rgb) # from 0 to 16777215 (hex: 0xFFFFFF)
    operate_on_bulb("set_rgb",str(color))

def yeelight_get_prop():
    ans = operate_on_bulb("get_prop","\"rgb\",\"bright\"")
    color_before = ans["color"]
    brightness_before = ans["brightness"]

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
        color = message["state"]["reported"]["color"]
        brightness = message["state"]["reported"]["brightness"]
        print (color,brightness)
        if brightness > 0:
            yeelight_on()
            yeelight_set_bright(brightness)
            yeelight_set_color(colors[color])
        else :
            yeelight_off()

#creating a client with client-id=mqtt-test
mqttc = mqtt.Client("qasemSa",True,None,mqtt.MQTTv31)

mqttc.on_connect = on_connect
mqttc.on_subscribe = on_subscribe
mqttc.on_message = on_message

#Configure network encryption and authentication options. Enables SSL/TLS support.
#adding client-side certificates and enabling tlsv1.2 support as required by aws-iot service
mqttc.tls_set("/Users/qasemsayah/Desktop/python_folder/NewSDK/root-CA.crt",
	            certfile="/Users/qasemsayah/Desktop/python_folder/NewSDK/3db3a6dca1-certificate.pem.crt",
	            keyfile="/Users/qasemsayah/Desktop/python_folder/NewSDK/3db3a6dca1-private.pem.key",
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

def publish_play_to_android():
    movie_name = kodiPlayerGetItemLabel()
    movie_prop = kodiPlayerGetProperty(1)
    movie_total_time = movie_prop["totaltime"]["hours"]+":"+movie_prop["totaltime"]["minutes"]+":"+movie_prop["totaltime"]["seconds"]
    movie_played_percentage = movie_prop["percentage"]
    message = json.dumps({
                         "IP": ip,
                         "movie_mode": "playing",
                         "movie_name": movie_name,
                         "movie_total_time": movie_total_time,
                         "movie_played_percent": movie_played_percentage,
                         "color": color,
                         "brightness": brightness,
                         })
        mqttc.publish("my/topic",message)

def publish_pause_to_android():
    movie_name = kodiPlayerGetItemLabel()
    movie_prop = kodiPlayerGetProperty(1)
    movie_total_time = movie_prop["totaltime"]["hours"]+":"+movie_prop["totaltime"]["minutes"]+":"+movie_prop["totaltime"]["seconds"]
    movie_played_percentage = movie_prop["percentage"]
    message = json.dumps({
                         "IP": ip,
                         "movie_mode": "paused",
                         "movie_name": movie_name,
                         "movie_total_time": movie_total_time,
                         "movie_played_percent": movie_played_percentage,
                         "color": color_before,
                         "brightness": brightness_before,
                         })
    mqttc.publish ("my/topic",message)

def buttonPressed():
    if first_launch :
        if "Connection refused" not in kodiPlayerGetItemLabel() :# if kodi is running
            if kodiIsActive():
                return
            else :
                #send init message
                kodiInit()
                publish_play_to_android()
                is_movie_playing = True
                first_launch = False
        else :
                #launch kodi
                pid = os.fork()
                if pid = 0:
                    os.system("kodi")
                else :
                    time.sleep(4)
                    kodiInit()
                    publish_play_to_android()
                    is_movie_playing = True
                    first_launch = False
    else if !is_movie_playing :
        #play movie
        publish_play_to_android()
        is_movie_playing = True
    else if is_movie_playing :
        #pause movie
        publish_pause_to_android()
        is_movie_playing = False

GPIO.add_event_detect(18, GPIO.FALLING, buttonPressed, bouncetime=2000)

first_launch = True
is_movie_playing = False
#while True:
#    input_state = GPIO.input(18)
#    if input_state == False:
#        print('Button Pressed on')
#        if flag :
#mqttc.publish("my/topic"," sss 1")
#        else :
#            mqttc.publish("my/topic",ip+" 0")
#        flag = 1-flag
#        time.sleep(1)

#automatically handles reconnecting+
mqttc.loop_forever()















        
