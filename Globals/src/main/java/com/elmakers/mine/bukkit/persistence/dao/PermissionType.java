package com.elmakers.mine.bukkit.persistence.dao;

// Enums don't need annotations to be persistable
public enum PermissionType
{
    ALLOW_ALL, DEFAULT, OPS_ONLY, PLAYER_ONLY
};