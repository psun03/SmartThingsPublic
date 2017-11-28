/**
 *  Garage_Door_Monitor.groovy
 *
 *  Copyright 2017 Dan G Ogorchock 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 *    2017-02-08  Dan Ogorchock  Original Creation
 *    2017-02-12  Dan Ogorchock  Modified to work with Ethernet based devices instead of ThingShield
 *    2017-02-26  Dan Ogorchock  Modified for Peter Sun's project
 *	  2017-03-01  Peter Sun      Modified GUI and renamed to Garage_Door_Monitor
 *	  2017-11-24  Peter Sun		 Added 'getStatus' function
 */
 
metadata {
	definition (name: "Garage_Door_Monitor", namespace: "ogiewon", author: "Dan Ogorchock") {
		capability "Configuration"
		capability "Switch"
		capability "Sensor"
		capability "Contact Sensor"

	}

    simulator {
 
    }

    // Preferences
	preferences {
    	section("Arduino Settings") {
		input "ip", "text", title: "Arduino IP Address", description: "ip", required: true, displayDuringSetup: true
		input "port", "text", title: "Arduino Port", description: "port", required: true, displayDuringSetup: true
		input "mac", "text", title: "Arduino MAC Addr", description: "mac", required: true, displayDuringSetup: true
     	}
     	section("Send Push Notification?") {
        input "sendPush", "bool", required: false, title: "Send Push Notification"
    	}
	}

	// Tile Definitions
	tiles {
         /*standardTile("switch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
			state "off", label: '${name}', action: "switch.on", icon: "st.doors.garage.garage-closed", backgroundColor: "#ffffff"
			state "on", label: '${name}', action: "switch.off", icon: "st.doors.garage.garage-open", backgroundColor: "#79b821"
		}*/
		standardTile("contact", "device.contact", width: 3, height: 3,  decoration: "flat") {
			state("open", label:'${name}', icon:"st.doors.garage.garage-open", backgroundColor:"#ffa81e")
			state("closed", label:'${name}', action:"getStatus", icon:"st.doors.garage.garage-closed", backgroundColor:"#79b821")
		}
		standardTile("configure", "device.configure", width: 3, height: 1, inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}

        main(["contact","configure"])
	}
}

def installed() {
    initialize()
}

def initialize() {
	log.debug "In initialize()"
    subscribe(contact, "contact.open", doorOpenHandler)
    subscribe(contact, "contact.closed", doorClosedHandler)
}

// parse events into attributes
def parse(String description) {
	//log.debug "Parsing '${description}'"
	def msg = parseLanMessage(description)
	def headerString = msg.header
	def result = createEvent(name: name, value: value)
    log.debug "results: $result"
    log.debug "results.isStateChange: $result.isStateChange"
    
	if (!headerString) {
		//log.debug "headerstring was null for some reason :("
    }

	def bodyString = msg.body
	log.debug "msg.body: $msg.body"
	if (bodyString) {
        log.debug "BodyString: $bodyString"
 		if(results.isStateChange){
     	   if(bodyString == "on"){
        		log.debug "Change to OPEN"   
        	    sendEvent(name: "contact", value: "open");
        	}
        	if(bodyString == "off"){
            	log.debug "Change to CLOSED"   
            	sendEvent(name: "contact",value:"closed");
        	}
	}        
    return result
	}
}

private getHostAddress() {
    def ip = settings.ip
    def port = settings.port

	log.debug "Using ip: ${ip} and port: ${port} for device: ${device.id}"
    return ip + ":" + port
}

def getStatus() {
	log.debug "In getStatus()"
	sendEthernet("ping")
}


def sendEthernet(message) {
	log.debug "Executing 'sendEthernet' ${message}"
	new physicalgraph.device.HubAction(
    	method: "POST",
    	path: "/${message}?",
    	headers: [ HOST: "${getHostAddress()}" ]
	)
}

// handle commands

def on() {
	log.debug "Executing 'switch on'"
	sendEthernet("switch on")
}

def off() {
	log.debug "Executing 'switch off'"
	sendEthernet("switch off")
}


def configure() {
	log.debug "Executing 'configure'"
	updateDeviceNetworkID()
}

def updateDeviceNetworkID() {
	log.debug "Executing 'updateDeviceNetworkID'"
    if(device.deviceNetworkId!=mac) {
    	log.debug "setting deviceNetworkID = ${mac}"
        device.setDeviceNetworkId("${mac}")
	}
}

def updated() {
	if (!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 5000) {
		state.updatedLastRanAt = now()
		log.debug "Executing 'updated'"
    	runIn(3, updateDeviceNetworkID)
	}
	else {
		log.trace "updated(): Ran within last 5 seconds so aborting."
	}
    initialize()
}

def doorOpenHandler(evt) {
	log.debug "In doorOpenHandler"
    if (sendPush) {
    	log.debug("Sending Notification")
        sendPushMessage("The Garage Door opened.")
    }
}

def doorClosedHandler(evt) {
	log.debug "In doorClosedHandler"
    if (sendPush) {
    	log.debug("Sending Notification")
        sendPushMessage("The Garage Door closed.")
    }
}