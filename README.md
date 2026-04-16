# TextAdventure
Overview 

This document outlines the revised class structure and variables for the text-based adventure game. While the original UML diagram included an Inventory class, the design has been simplified based on instructor feedback. Instead of using a separate Inventory class, inventory management is handled through the Item class, with ArrayList<Item> stored directly in both the Player and Room classes. In addition, the relationship between Puzzle and Room is modeled as a simple association rather than a more complex relationship. 

Shape 

Player Class 

Variables 

String name  

int health  

int maxHealth  

int attackPower  

Room currentRoom  

ArrayList<Item> inventory  

int score  

boolean isAlive  

Description 

The Player class represents the user playing the game and stores all major gameplay information related to the character. 

name: Stores the player’s name  

health: Current health points  

maxHealth: Maximum health limit  

attackPower: Base attack damage  

currentRoom: The room the player is currently in  

inventory: Stores all items the player is carrying  

score: Tracks progress or achievements  

isAlive: Determines if the player is still alive in the game  

Shape 

Room Class 

Variables 

String roomID  

String name  

String description  

String northExit  

String southExit  

String eastExit  

String westExit  

String status  

String requiredItemID  

ArrayList<String> inspectableObjects  

ArrayList<Item> items  

Monster monster  

Puzzle puzzle  

boolean isVisited  

boolean isLocked  

Description 

The Room class represents each location in the game world. It stores room details, exits, items, and any puzzle or monster located there. 

roomID: Unique identifier for the room  

name: Name of the room  

description: Text shown when the player enters or examines the room  

northExit, southExit, eastExit, westExit: Room connections in each direction  

status: Tracks the current state of the room  

requiredItemID: ID of the item needed to unlock or access the room  

inspectableObjects: Objects in the room that the player can inspect  

items: Stores items currently available in the room  

monster: Enemy located in the room  

puzzle: Puzzle associated with the room  

isVisited: Tracks whether the room has already been visited  

isLocked: Indicates whether the room is locked  

Relationship Note 

The relationship between Room and Puzzle is a simple association. A room may contain a puzzle, but the puzzle remains its own separate class. 

Shape 

Puzzle Class 

Variables 

String puzzleID  

String name  

String prompt  

ArrayList<String> solutions  

Item reward  

String successMessage  

int attemptsRemaining  

int maxAttempts  

boolean isSolved  

Description 

The Puzzle class represents a challenge the player must solve in order to progress or receive a reward. 

puzzleID: Unique identifier for the puzzle  

name: Name of the puzzle  

prompt: The question or challenge shown to the player  

solutions: List of acceptable answers  

reward: Item given after the puzzle is solved  

successMessage: Message displayed when the puzzle is completed successfully  

attemptsRemaining: Number of tries left  

maxAttempts: Maximum number of attempts allowed  

isSolved: Tracks whether the puzzle has already been solved  

Shape 

Monster Class 

Variables 

String monsterID  

String name  

String description  

int health  

int attack  

String floor  

Item dropItem  

String specialAbility  

Description 

The Monster class represents enemies encountered by the player. 

monsterID: Unique identifier for the monster  

name: Monster name  

description: Description shown when the monster appears  

health: Current health points  

attack: Damage dealt by the monster  

floor: Indicates the floor or level where the monster appears  

dropItem: Item dropped after defeating the monster  

specialAbility: Unique attack or skill used by the monster  

Shape 

Item Class 

Variables 

String itemID  

String name  

String type  

String description  

String useEffect  

String obtainedFrom  

boolean isConsumable  

boolean isEquippable  

boolean isKeyItem  

int attackBonus  

Description 

The Item class represents any object the player can collect, use, equip, or inspect. Since the design removes a separate Inventory class, item-related functionality becomes even more important here. 

itemID: Unique identifier for the item  

name: Name of the item  

type: Category such as weapon, key item, or consumable  

description: Text description of the item  

useEffect: Describes what happens when the item is used  

obtainedFrom: Shows where the item came from  

isConsumable: Indicates whether the item can be used once  

isEquippable: Indicates whether the item can be equipped  

isKeyItem: Indicates whether the item unlocks rooms or puzzles  

attackBonus: Extra damage given by the item if used as a weapon  

Design Note 

Because the Inventory class was removed, methods that manage items can be handled directly through the Item class and through the ArrayList<Item> variables in Player and Room. 

Shape 

Item Subclasses 

KeyItem 

Represents items used to unlock rooms, doors, or puzzles. No extra variables are required beyond those inherited from Item. 

WeaponItem 

Represents weapons that increase the player’s attack ability. No extra variables are required beyond those inherited from Item. 

ConsumableItem 

Represents items that can be used once for an effect such as healing or restoring energy. No extra variables are required beyond those inherited from Item. 

AccessItem 

Represents items required to gain access to special areas. No extra variables are required beyond those inherited from Item. 

Shape 

GameManager Class 

Variables 

Map<String, Room> rooms  

Player player  

Room currentRoom  

boolean isRunning  

boolean saveManager  

Scanner scanner  

Description 

The GameManager class controls the overall game flow and handles the main logic of the adventure. 

rooms: Stores all rooms in the game  

player: The player object  

currentRoom: The active room being used in the game  

isRunning: Controls whether the game loop continues  

saveManager: Tracks saving functionality  

scanner: Used to collect user input  

Shape 

Design Decisions 

1. Inventory Simplification 

A separate Inventory class is not necessary for this design. Instead: 

Player stores an ArrayList<Item> called inventory  

Room stores an ArrayList<Item> called items  

This makes the design simpler while still allowing full inventory functionality. 

2. Puzzle–Room Relationship 

The relationship between Puzzle and Room is a simple association. 
This means: 

a room may contain a puzzle  

the puzzle is still its own class  

the relationship is shown as a solid line with an arrow in UML  

 
