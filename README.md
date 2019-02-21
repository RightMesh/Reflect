# Reflect [![Build status](https://travis-ci.com/RightMesh/Reflect.svg?branch=master)](https://travis-ci.com/RightMesh/Reflect)

## What is this?

Reflect is a simple Android application used by the RightMesh team to test network reach and performance in the field. Users can select another device on the mesh from a drop-down menu, then tap the floating "send" button to send a ping to that device. That device will return the ping when it receives it. The user's device will log when it sent the ping, and update that log when the ping is returned. The target device will log when it receives a ping. The RightMesh settings page can be accessed by long-pressing the send button.

Using this app allows a user to place multiple devices across an area, and still test that a device across the mesh can be reached. It was initially developed in [Rigolet, Labrador](http://www.townofrigolet.com/home/) to test the feasibility of [using RightMesh to provide connectivity to remote communities](https://medium.com/@compscidr/the-state-of-connectivity-in-canadas-remote-communities-3a8c477c2194).

## How do I build it?

Reflect is built in Android Studio, and should be able to be opened once this repo has been cloned. Note that you will have to sign up for a RightMesh developer account in order to download our library and license verification Gradle plugin - please check out [https://rightmesh.io/developers](https://rightmesh.io/developers) for more information.

## What is RightMesh?

RightMesh is an SDK that is trying change the paradigm from “Always Connected to the Internet” and let everyone simply be “Always Connected” - to people, to devices, to our communities, to what matters in our world. RightMesh connects smartphones even when the Internet and mobile data can’t. [Check out our website for more details!](https://www.rightmesh.io)
