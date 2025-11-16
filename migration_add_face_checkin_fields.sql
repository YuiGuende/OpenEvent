-- Migration: Add face check-in fields to customer table
-- Run this SQL script to add avatarUrl and faceRegistered columns

ALTER TABLE customer 
ADD COLUMN avatar_url VARCHAR(500) DEFAULT NULL COMMENT 'URL to customer profile photo for face recognition',
ADD COLUMN face_registered BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether customer has registered face for check-in';

-- Add index for faster queries
CREATE INDEX idx_customer_face_registered ON customer(face_registered);

-- Optional: Update existing customers to set faceRegistered = false if they have avatar
-- UPDATE customer c
-- JOIN user u ON c.user_id = u.user_id
-- SET c.face_registered = FALSE
-- WHERE u.avatar IS NOT NULL AND u.avatar != '';

