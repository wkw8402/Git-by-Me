# Gitlet Design Document
author: Kyung-Wan Woo

## Design Document Guidelines

Please use the following format for your Gitlet design document. Your design
document should be written in markdown, a language that allows you to nicely 
format and style a text file. Organize your design document in a way that 
will make it easy for you or a course-staff member to read.  

## 1. Classes and Data Structures

### Commit 
#### This class stores log messages, commit date, references to parent commits and tracked blobs.
#### Instance Variables
* Message - contains the message of a commit.
* Timestamp - time at which a commit was created. Assigned by the constructor.
* Parent - parent commit of the commit object.
* Second Parent - second parent commit for merges
* Blobs - contains a Treemap with its key equal to filename and its values equal to the blob ID (SHA-1 hash of blob contents)
* UID - unique id of a commit by using sha-1 to metadata and parent/blobs reference

### Main
#### This class runs the input gitlet commands.
#### Instance Variables
* currentBranch - name of the current branch

## 2. Algorithms

### Main Class
#### 1. init()
* creates a new repository folder inside .gitlet
* creates a initial commit (all repositories share this commit = same UID)
* creates a master branch
* head pointer to initial commit

#### 2. add()
* adds a file to staging area inside .gitlet
* do not stage or ***unstage*** if current commit has same version of file
* unstage = changed, added, changed back

#### 3. commit()
* creates a new commit by copying the current commit as a parent
* date changes for each new commit
* new commit = new node in commit tree
* head pointer to new commit
* only if the file the parent commit is tracking has been updated, 
* clear staging area everytime
* commit includes SHA-1 ID

#### 4. rm()
* unstage the file from staging area
* if it is in current commit, stage it for removal
* remove the file from .gitlet = working directory

#### 5. log()
* display commit id, timestamp, message
* from the current head commit to the initial commit
* timestamp reflecting current time zone
* in case of global-log, display all commits ever made without any order

#### 6. find() 
* Prints out the ids of all commits that have the given commit message
* iterate through the commit folder in .gitlet

#### 7. status()
* display the branch (current with "*" on the front)
* display the staged files, removed files
* extra credit below - 1 point 
* display modified but not staged files = (tracked, modified, not staged) or (staged for addition but different ***or*** deleted in cwd) or (not staged for removal, tracked in current commit but deleted in cwd)  
* display untracked files = present in cwd but neither staged nor removed

#### 8. checkout()
* overwrite file of cwd with the contents in current head commit
* or with the contents in commit with gived id
* or with the contents in head of given branch
* if file in current branch is not present in checked-out branch, delete

#### 9. branch()
* create a branch that point to current head node
* a name for a reference to a commit code 

#### 10. rm_branch()
* delete the pointer of this branch to any commit;

#### 11. reset()
* removes tracked files that are not present in the given commit.
* clear staging area
* move current branch's head to that node

#### 12. merge()
* find split point = latest common ancestor
1. modified in other but not head = other
2. modified in head but not other = head
3. modified in other and head in ***same way*** = same
4. modified in other and head in ***different way*** = conflict
5. not in split nor other but in head = head
6. not in split nor head but in other = other
7. unmodified in head but not present in other = remove
8. unmodified in other but not present in head = remain removed
* in the case of updating with other = ***check out*** from the commit at the front of the given branch
* in merge conflict, replace the contents of conflicted file (treat deleted file as an empty file with no new line)
* merge commit - first and second parent.

### Commit Class
#### 1. commit(message, parent)
* timestamp created when a new instance of this class is created.

## 3. Persistence

#### Main class
Create a new file directory for current working directory of .gitlet folder

####'java gitlet.Main init'
* In order to persist the new repository folder, I will need to first make a folder in .gitlet directory .
Set up persistence by using Join method from the Utils class and repository.mkdir.
Then, I create a commit folder inside the new repo and save the initial commit with its SHA-1 ID as its file name.
SHA-1 ID will solely be based on message, date, parent and blobs.
Making sure that all commit and blobs object implements Serializable interface.

####'java gitlet.Main add [file name]'
* Use Join method from the Utils class to add a copy to staging area folder in .gitlet
* use the readObject or readContentsAsString method from the Utils class to read and compare the file contents in current commit and in cwd

####'java gitlet.Main commit [message]'
* Create a new commit file and use writeObject/writeContents() method from the Utils class to write metadata and String references.

####'java gitlet.Main rm [file name]'
* Remove the file from the staging area folder
* Add to staging folder for removal and remove the file from the cwd

####'java gitlet.Main log/global-log/find/status/checkout'
* iterate through commit folder (from the current head commit) and output the result of readObject() method of commit files in commit folder
* check the commit message and branch name using readContents() method from Utils class

####'java gitlet.Main branch [branch name] / rm-branch / reset [commit id]'
* find the current head or given commit in the commit folder and use writeContents() to add/remove a reference of new branch pointer to this commit or call checkout.

####'java gitlet.Main merge [branch]'
* in case of second parent or merge conflict, update the contents of conflicted file with writeContents() method.


## 4. Design Diagram
![Design Document](gitlet-design.png)

