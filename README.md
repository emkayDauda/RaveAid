#RAid
RAid (RaveAid) is a simple application that encourages anyone with a story, amateur or expert writers,
to upload their stories, share them with the world and get paid.

##Getting Started

These instructions will help you get a copy of the project up and running on your local machine for development and 
testing purposes. 

###Prerequisites
  * Android Studio 3.2
  * Android SDK 
  * Java JDK
  
#Installing
To get a local copy of the project, follow these steps:
1. Clone or fork the repository by running from a terminal:
`git clone https://github.com/emkayDauda/RaveAid.git`

2. Import the project into android studio.
    1. In Android Studio, go to File -> New -> Import Project
    2. Follow the dialog to choose the folder where the project was cloned.
    3. You might need internet access on initial gradle build. 

3. Add Firebase to project.
    1. To do this, simply go to tools -> Firebase Assistant
    2. Select first `FirebaseAuth` then `RealtimeDatabse` on the right pane.
    3. Follow the instructions in both cases.
    4. Clean and rebuild project just to be safe.
    
**Note**: A sample release apk can be found [here](https://docs.google.com/uc?export=download&id=1h4lGuEazZk9cpdIxNI5dL6v7XxYrkKdO)
    
#Built With
* [Android Support Library](https://developer.android.com/topic/libraries/support-library/revisions) - Support Library 27
* [Firebase](https://firebase.google.com/) - Authentication and data persistence.
* [FirebaseUI](https://github.com/firebase/FirebaseUI-Android) - For fast UI build (Auth and Database)

#Authors
* [Maaruf Dauda](https://github.com/emkayDauda)