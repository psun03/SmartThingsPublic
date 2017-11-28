/**
 *  Trash_Monitor.groovy
 *
 *  Copyright 2017 Peter Sun
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
 *    2017-03-01  Peter Sun  Original Creation
 *	  2017-04-29  Peter Sun  Deleted unnescessary capability
 *							 Modified cron expression
 */
 
metadata {
	definition (name: "Trash_Monitor", namespace: "psun03", author: "Peter Sun") {
		capability "Switch"
        capability "Configuration"
	}

    simulator {
 
    }

    // Preferences
	preferences {
  }
//, backgroundColor: "#ffffff", nextState: "on"
//, backgroundColor: "#00A0DC", nextState: "off"
	// Tile Definitions
	tiles (scale: 2){
		standardTile("button", "device.switch", width: 6, height: 2,decoration:"flat") {
			state "off", label:"Trash", icon: "http://studiosunmedia.com/trash_webservice/images/trash_bin.png", backgroundColor: "#b5d7ff"
			state "on", label: "Recycle", icon: "http://studiosunmedia.com/trash_webservice/images/recycle_bin.png", backgroundColor: "#b8ffb4"
		}
        standardTile("check", "device.configure", width: 6, height: 2, canChangeIcon: true,  decoration: "flat") {
			state("Check", label: "Check", action: "configuration.configure")
		}
		main "button"
		details (["button","check"])
	}
}
def initialize() {
	schedule("0 0 23 * * TUE *",sendEthernet)
}


def sendEthernet() {
    def params = [
            uri: "http://studiosunmedia.com",
            path: "/trash_webservice/trash_webservice.php"
        ]

        try {
            httpGet(params) { resp ->
                    resp.headers.each {
                    log.debug "name: ${it.name} | value: ${it.value}"
                }
                def value = resp.data["answer"]
                def status = resp.data["status"] 
                log.debug "value: ${value}"
                log.debug "status: ${status}"
                if(value == "T") {
                	log.debug "Trash"
                    sendEvent(name: "switch", value: "off")
                }
                else if(value == "TR") {
                	log.debug "Recycle"
                    sendEvent(name: "switch", value: "on")
                }
            }
        }
        catch (e) {
            log.error "something went wrong: $e"
        }
}	

// handle commands

def configure() {
	sendEthernet()
}
