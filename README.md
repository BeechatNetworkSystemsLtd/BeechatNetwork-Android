# Beechat Network for Android
## _Android version of the open source, encrypted, peer-to-peer, Beechat app._

![BNSLTD](https://beechat.network/wp-content/uploads/2021/02/powered-by-1.png)
![License](https://img.shields.io/badge/License-GPLv2-blue)

## Introduction

Beechat is an open source system which allows people to securely communicate over encrypted peer-to-peer, mesh, radio connections, and without relying on commercial infrastructure.

* **No SIM cards, no Internet required.**
* **No shady tracking.**
* **We all fully own the system, this way, no one can destroy it.**

If you want to send a message to someone outside your range, people in between you two can act as a bridge, without ever knowing the contents of the encrypted message.

[Download pre-release demo APK](https://github.com/BeechatNetworkSystemsLtd/BeechatNetwork-Android/releases)
-------------------

Code 
-------------------
* Java: 
    1. MainActivity.java contains code for displaying and interacting with the main application window. At startup, it scans and displays a list of devices available for communication. You can also refresh the list of available devices using the "Refresh" button.
    2. ChatActivity.java contains code for connecting to an available device from the main window and organizing a text chat. The screen contains information about the connection and the ability to return to the main application window.
   The corresponding XML files are responsible for the design and structure of the activity elements: activity_main.xml and activity_chat.xml.

* client_message_box.xml and server_message_box.xml files are responsible for displaying message boxes between the sending and receiving parties.
* The digi_icon.png file is the application icon.
* The strings.xml file is designed to save strings of constant strings displayed in the application.
* The styles.xml file is for setting colors inside the application.
* The AndroidManifest.xml file is intended for grouping files into one structure and the ability to add permissions within the application.
* The app/build.gradle and project/build.gradle files are intended for building libraries and applications, respectively.
* The settings.gradle, local.properties and gradle.properties files contain different settings for gradle and project.
* The application consists of the following directories:
- BeechatNetwork. Main project directory. 
  - BeechatNetwork/app. Main app directory.
    - BeechatNetwork/app/src/main/java. Source code directory.
    - BeechatNetwork/app/main/java/res. Styles code directory.
  - BeechatNetwork/xbee_android_library. Directory contains code of Xbee Library for Android.
  - BeechatNetwork/gradle. Gradle settings directory.

Timeline
-------------------
* Alpha: basic demo radio chat app without encryption.
* Beta: adding secure encryption & signature layer to the app - production ready.
* Launch: Authenticated, unbannable broadcasts, multiple identities per user, and secure file sending implemented.

Threat model
-------------------
* __MITM Attacks__: we assume an intermediary node could spoof the 64 bit hardware address (HWAddr) of both Alice’s and Bob’s radios. The bad actor can then conduct a man in the middle attack.
__Solution__: By having Alice and Bob encrypt their messages, and share their public keys through QR codes in person, even if the bad actor spoofs the HWAddr, they will not be able to see contents of the message.

* __DoS attack__: an attacker can spam the network and flood it with packets by modifying the open source app and creating an infinite loop sending packets. 
Attack limits: the bad actor’s packets will only reach 20 hops or less in case the network has fewer nodes. 
__Solution__: unknown. One possibility would be automatic agreement on channel switching based on high congestion. 

* __Harvest and decrypt attack__: encrypted messages exchanged between Alice and Bob could be harvested and decrypted with a sufficiently powerful quantum computer through Shor’s Algorithm. 
__Solution__: by implementing post-quantum cryptography, not even a quantum computer can decrypt the messages as a discrete logarithm attack is not possible. In addition, the attacker would need to have the intended recipient’s public key, which is unlikely due to the QR codes being shared in person. 

Features
-------------------
**Alpha Features**
| Feature | Description | 
| ------ | ------ |
| Basic radio chat | Select from nodes in your network and start a chat | 

**Beta Features** Q3 2021
| Category | Feature | Description |
| ------ | ------ | ------ |
| UX | Notifications |  **Added on 2/9/2021** add system notifications for messages received when the device is locked or in another app. |
| Security | Extended error catching and debug log system | **Added on 2/9/2021** add better error catching to prevent unexpected hang-ups. |
| Optimisations | Separate thread for listnodes | run listnodes method on a separate thread to prevent app hang-up. |
| UX | Splash screen | add a splash screen on startup with progress bar. |
| Misc. | Receipts | Sent & read receipts as well as timestamped  messages. |
| Misc. | SQLite DB | add database file to be able to save conversations with and other data persistently. |
| UX | Saved conversations window | add a new window containing saved conversations & broadcasts. |
|Security | kyber-JNI implementation | implementing asymmetric encryption of messages utilising our [kyber-JNI](https://github.com/BeechatNetworkSystemsLtd/kyber-JNI) repo. | 
|Security | dilithium-JNI implementation | implementing digital signature scheme authenticated messages utilising our [dilithium-JNI](https://github.com/BeechatNetworkSystemsLtd/dilithium-JNI) repo. |
| UX | Settings window | baud rate slider, language, dark mode, wipe database. |
| UX | Refresh animation | show a loading animation when clicking on the refresh button. |

__Public launch__ (Q4 2021)

 Category | Feature | Description |
| ------ | ------ | ------ |
|Security | Multiple identities | switch between different kyber-dilithium keypairs within the application. | 
|Misc. | Signed broadcasts with dilithium-JNI | allow subscriber nodes to subscribe to publisher nodes. Publisher nodes can then send signed public messages to their subscribers. | 
|Misc. | File sending | allow sending of images, docx, pdf and other files in an encrypted manner. | 



License Information
-------------------

The hardware is released under the [GNU General Public License, version 2](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html).

Distributed as-is; no warranty is given.
