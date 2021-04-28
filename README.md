## Introduction

PRIME aims to enable physicians in the better management of the disease. This includes five main components:
1) a decision support tool based on PD treatment guidelines and ontologies
2) a prescription support tool which includes drug to drug, drug to gene and drug to protiens interaction checkers
3) automatic identification of similar cases with machine learning
4) functional tests to assess motor symptoms, specifically gait and tremor, using sensors
5) a physician dashboard to more easily monitor patient status and disease progress and make treatment decisions 

The specific repository provides the code and description for the 4) functional tests and specifically for the sensor data collection app


## Motor symptoms evaluation component
Data collection

![image](https://user-images.githubusercontent.com/13904310/116374501-0108cc00-a817-11eb-82ca-b745b08294ec.png)













Fig. 1: Communication blueprint of the mobile application

The mobile application is responsible for collecting and synchronizing the data from both insole pressure sensors and IMU sensor. 
The mobile application constructs two threads, one for each sensor.
The thread responsible for the insole collection implements the Moticon communication protocol, as presented in Fig. 2.
 
 

![image](https://user-images.githubusercontent.com/13904310/116374589-17af2300-a817-11eb-804f-648e932e98d9.png)
 
Fig. 2: Message sequence chart for live data transmission

As far as the communication handler responsible for IMU data, the mobile application automatically seeks the registered (via MAC address) IMU sensor and establish a connection for data retrieval.
Both handlers are utilizing BLE communication protocol and the update rate is set to 100Hz.
