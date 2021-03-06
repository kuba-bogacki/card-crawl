package com.example.dungeaoncrawler.logic.actors;

import com.example.dungeaoncrawler.logic.Cell;

import java.util.concurrent.ThreadLocalRandom;

public class Skeleton extends Enemy {

    public Skeleton(int health, int resistance, int armor, int exp, int attackRound, Cell cell) {
        super(health, resistance, armor, exp, attackRound, cell, "skeleton", ActorType.SKELETON, new String[]{"poison", "damage"},
                0, 0, 3, 8, 1, 3);
    }
}
