# Naive-Bayes-Multi-Agents
This Project is Multi-Agent of Naive Classifiers System Implemented using JADE(Java Agent Development Framework).

##Agent Specification
- Each Agent holds a Naive Bayes Classifier targeting a data file (denoting different enviroment of data).
- Agent with localName `master` is used as Master Agent, At least one agent must be a master.

##Classification Process
- Test tuple is given to master agent to be classifed.
- Master Agent Classifiy the Tuple on its data.
- Master Agent Communicate with all other Naive-Bayes agents in the system to get their classfication to the tuple.
- Finally, Master Agent use voting to declare the final classification result.

##Dependencies
- Java Runtime Enviroment.
- JADE (Included on the Project)

##Running Steps
### 1. Open Terminal and execute config.bash file
```
$ [path_of_config.bash_file]
```
This will open JADE GUI , Activate master Agent with data.txt (Now master agent is ready to take test tuple)

###2. Start Adding Agents Using JADE GUI
```
Click Start New Agent
Agent Name: Any name except 'master'
Class Name: NaiveBayesAgent
Arguments: <txt_data_file_name> eg. data,data1
```

###3. Send Test Tuple On terminal on active master agent

## Data Files
- Data Files are assumed to be stored in project directory.
- There 4 sample data files in project directory.
- Last Column Assumed to be class label.
