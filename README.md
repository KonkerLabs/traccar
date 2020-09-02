# [Traccar](https://www.traccar.org)
[![Build Status](https://travis-ci.org/traccar/traccar.svg?branch=master)](https://travis-ci.org/traccar/traccar)

## Overview

Traccar is an open source GPS tracking system. This repository contains Java-based back-end service. It supports more than 170 GPS protocols and more than 1500 models of GPS tracking devices. Traccar can be used with any major SQL database system. It also provides easy to use [REST API](https://www.traccar.org/traccar-api/).

Other parts of Traccar solution include:

- [Traccar web app](https://github.com/traccar/traccar-web)
- [Traccar Manager Android app](https://github.com/traccar/traccar-manager-android)
- [Traccar Manager iOS app](https://github.com/traccar/traccar-manager-ios)

There is also a set of mobile apps that you can use for tracking mobile devices:

- [Traccar Client Android app](https://github.com/traccar/traccar-client-android)
- [Traccar Client iOS app](https://github.com/traccar/traccar-client-ios)

## KONKER Integration 

To integrate with KONKER platform, you can use the Dockerized version of this Traccar server with KonkerHandler.

### Architecture

To allow the integration, KONKER platform uses the expansaible Handler configuration of Traccar platform.
It defines a new KonkerHandler class that handles all messages received by tracking devices (with support for all protocols used by Traccar)
and encapsulates this data (Position) to pass back to the platform, acting as a GATEWAY device.

All major information required to work with tracking systems are processed and additional data processed by each
protocol is attached as an attribute ("attr") field on JSON sent to the platform

The conifguration and operation is simple:

* execute the konkerlabs/konker-mis:traccar-4.8.2 server (available at docker hub)
* define which Gateway credential should be used -- KONKER_AUTH -- environment 
* export ports to handle specific protocols on the platform

### Configuration

* access the developer portal at Konker platform 
* create a new Gateway on the account / application that will receive all trackers
* generate a token for this gateway device -- write down it's access token (TOKEN)
* on a machine with Docker, execute the traccink integration server, informing the TOKEN received as the token to be used to proceed with the integration, as shown bellow:

docker run \
--detach \
--restart always \
--name traccar \
--hostname traccar \
-p 5000-5150:5000-5150 \
-p 5000-5150:5000-5150/udp \
-v /home/rancher/logs:/opt/traccar/logs:rw \
-e KONKER_AUTH="Bearer <GATEWAY CREDENTIAL>" \
konkerlabs/techlab-misc:traccar-4.8.4

* get the Public Server IP where you are running this server
* if you are running this server on a public cloud (AWS, DigitalOcean), please make sure that network ports are accessible by the world ...
* configure your device to access the server IP and protocol-specific port 
    * take a look @ https://www.traccar.org/devices/ for specific devices
    * if no device make sense, please use the Traccar information to identify specific protocol:
        * https://www.traccar.org/identify-protocol/
        * https://www.traccar.org/forums/forum/devices/
        * to have access to the tracking log ... use "docker log" on the running instance of your server

### Notes when using DIGITAL OCEAN

* create a container-based droplet (RancherOS) -- mininum 10USD/mon machine
* log and enable swap file - 4Gb as stated on https://www.digitalocean.com/community/tutorials/how-to-add-swap-on-centos-7
* 

### Usage

* once configurated the integration, all tracking devices that access this bridge, will automatically create a new device (with IMEI as its ID) 
* each device will receive on the "location" channel all data for this device
    * ultra-specific information for each device will be on the "attr" field of JSON payload -- some alarms will display there 
        depending on the specific device / protocol generation  


## Features

Some of the available features include:

- Real-time GPS tracking
- Driver behaviour monitoring
- Detailed and summary reports
- Geofencing functionality
- Alarms and notifications
- Account and device management
- Email and SMS support

## Build

Please read [build from source documentation](https://www.traccar.org/build/) on the official website.

specific local build / publishing ...

- to compile locally 
    gradle build

- to create the docker image to be published; replace #.#.# by new version #
    docker build . -t konkerlabs/techlab-misc:traccar-#.#.#

- to publish docker image 
    docker push konkerlabs/techlab-misc:traccar-#.#.#

## Team

- Anton Tananaev ([anton@traccar.org](mailto:anton@traccar.org))
- Andrey Kunitsyn ([andrey@traccar.org](mailto:andrey@traccar.org))

## License

    Apache License, Version 2.0

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
