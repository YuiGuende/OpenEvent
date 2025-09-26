-- MySQL dump 10.13  Distrib 8.0.36, for Win64 (x86_64)
--
-- Host: localhost    Database: openevent
-- ------------------------------------------------------
-- Server version	8.0.37

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `account`
--

DROP TABLE IF EXISTS `account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account` (
  `account_id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('ADMIN','HOST','USER') NOT NULL,
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `UKq0uja26qgu1atulenwup9rxyr` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account`
--

LOCK TABLES `account` WRITE;
/*!40000 ALTER TABLE `account` DISABLE KEYS */;
INSERT INTO `account` VALUES (1,'admin@gmail.com','$2a$10$mW0wPx/RGy5Tc2j3llkf5Oe8pWExQzegLIcApf5X32/7Lzt8CLFO2','ADMIN'),(2,'phongle@gmail.com','$2a$10$tB3RbXF9tvPn4ajHto7K..RkBcgMYQPC6nEppWsbVind1lwzR7sku','USER');
/*!40000 ALTER TABLE `account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `admin`
--

DROP TABLE IF EXISTS `admin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin` (
  `admin_id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(100) DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `account_id` int NOT NULL,
  PRIMARY KEY (`admin_id`),
  UNIQUE KEY `UKkpqdh7cixswtifyy72wpkxu1m` (`account_id`),
  CONSTRAINT `fk_admin_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`account_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin`
--

LOCK TABLES `admin` WRITE;
/*!40000 ALTER TABLE `admin` DISABLE KEYS */;
INSERT INTO `admin` VALUES (1,'admin@gmail.com','Administrator',NULL,1);
/*!40000 ALTER TABLE `admin` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `event`
--

DROP TABLE IF EXISTS `event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event` (
  `event_type` varchar(31) NOT NULL,
  `id` int NOT NULL,
  `benefits` text,
  `created_at` datetime(6) NOT NULL,
  `description` text,
  `ends_at` datetime(6) NOT NULL,
  `enroll_deadline` datetime(6) NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `learning_objects` text,
  `points` int DEFAULT NULL,
  `public_date` datetime(6) DEFAULT NULL,
  `starts_at` datetime(6) NOT NULL,
  `status` enum('CANCEL','DRAFT','FINISH','ONGOING','PUBLIC') NOT NULL,
  `event_title` varchar(150) NOT NULL,
  `competition_type` varchar(255) DEFAULT NULL,
  `prize_pool` varchar(255) DEFAULT NULL,
  `rules` text,
  `culture` varchar(255) DEFAULT NULL,
  `highlight` text,
  `materials_link` varchar(255) DEFAULT NULL,
  `topic` varchar(255) DEFAULT NULL,
  `parent_event_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKrdcxmu00l3t9wpwypmt9pepq0` (`parent_event_id`),
  CONSTRAINT `FKrdcxmu00l3t9wpwypmt9pepq0` FOREIGN KEY (`parent_event_id`) REFERENCES `event` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `event`
--

LOCK TABLES `event` WRITE;
/*!40000 ALTER TABLE `event` DISABLE KEYS */;
INSERT INTO `event` VALUES ('Event',1,'Thưởng thức âm nhạc, giao lưu nghệ sĩ.','2025-09-17 16:44:34.783519','Đêm nhạc đặc biệt cùng các nghệ sĩ nổi tiếng.','2025-09-28 22:00:00.000000','2025-09-25 23:59:59.000000','https://example.com/music.jpg','Trải nghiệm sân khấu chuyên nghiệp',50,'2025-09-20 10:00:00.000000','2025-09-28 19:00:00.000000','DRAFT','Live Music Night',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('Event',2,'Thưởng thức âm nhạc, giao lưu nghệ sĩ.','2025-09-18 23:34:39.015753','Đêm nhạc đặc biệt cùng các nghệ sĩ nổi tiếng.','2025-09-28 22:00:00.000000','2025-09-25 23:59:59.000000','https://example.com/music.jpg','Trải nghiệm sân khấu chuyên nghiệp',50,'2025-09-20 10:00:00.000000','2025-09-28 19:00:00.000000','DRAFT','Live Music Night',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('Event',3,'Thưởng thức âm nhạc, giao lưu nghệ sĩ.','2025-09-18 23:42:29.488798','Đêm nhạc đặc biệt cùng các nghệ sĩ nổi tiếng.','2025-09-28 22:00:00.000000','2025-09-25 23:59:59.000000','https://example.com/music.jpg','Trải nghiệm sân khấu chuyên nghiệp',50,'2025-09-20 10:00:00.000000','2025-09-28 19:00:00.000000','DRAFT','Live Music Night',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('Event',4,'Thưởng thức âm nhạc, giao lưu nghệ sĩ.','2025-09-18 23:44:15.361565','Đêm nhạc đặc biệt cùng các nghệ sĩ nổi tiếng.','2025-09-28 22:00:00.000000','2025-09-25 23:59:59.000000','https://example.com/music.jpg','Trải nghiệm sân khấu chuyên nghiệp',50,'2025-09-20 10:00:00.000000','2025-09-28 19:00:00.000000','DRAFT','Live Music Night 2',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('MUSIC',5,'Thưởng thức âm nhạc, giao lưu nghệ sĩ.','2025-09-19 07:22:35.088783','Đêm nhạc đặc biệt cùng các nghệ sĩ nổi tiếng.','2025-09-28 22:00:00.000000','2025-09-25 23:59:59.000000','https://example.com/music.jpg','Trải nghiệm sân khấu chuyên nghiệp',50,'2025-09-20 10:00:00.000000','2025-09-28 19:00:00.000000','DRAFT','Live Music Night 3',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('FESTIVAL',6,'Thưởng thức âm nhạc, giao lưu nghệ sĩ.','2025-09-19 08:35:35.204829','Đêm nhạc đặc biệt cùng các nghệ sĩ nổi tiếng.','2025-09-28 22:00:00.000000','2025-09-25 23:59:59.000000','https://example.com/music.jpg','Trải nghiệm sân khấu chuyên nghiệp',50,'2025-09-20 10:00:00.000000','2025-09-28 19:00:00.000000','DRAFT','Live Music Night 3',NULL,NULL,NULL,'Korea',NULL,NULL,NULL,NULL),('FESTIVAL',7,'Thưởng thức âm nhạc, giao lưu nghệ sĩ.','2025-09-19 22:06:34.585663','Đêm nhạc đặc biệt cùng các nghệ sĩ nổi tiếng.','2025-09-28 22:00:00.000000','2025-09-25 23:59:59.000000','https://example.com/music.jpg','Trải nghiệm sân khấu chuyên nghiệp',50,'2025-09-20 10:00:00.000000','2025-09-28 19:00:00.000000','DRAFT','Live Music Night 3',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),('WORKSHOP',8,'Thưởng thức âm nhạc, giao lưu nghệ sĩ.','2025-09-19 22:08:50.603020','Đêm nhạc đặc biệt cùng các nghệ sĩ nổi tiếng.','2025-09-28 22:00:00.000000','2025-09-25 23:59:59.000000','https://example.com/music.jpg','Trải nghiệm sân khấu chuyên nghiệp',50,'2025-09-20 10:00:00.000000','2025-09-28 19:00:00.000000','DRAFT','Live Music Night 3',NULL,NULL,NULL,NULL,NULL,NULL,'How to become millionare',NULL);
/*!40000 ALTER TABLE `event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `event_image`
--

DROP TABLE IF EXISTS `event_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_image` (
  `id` int NOT NULL AUTO_INCREMENT,
  `main_poster` bit(1) NOT NULL,
  `order_index` int NOT NULL,
  `url` varchar(255) DEFAULT NULL,
  `event_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9oirj7cwmu7k91vr0m13hqh8b` (`event_id`),
  CONSTRAINT `FK9oirj7cwmu7k91vr0m13hqh8b` FOREIGN KEY (`event_id`) REFERENCES `event` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `event_image`
--

LOCK TABLES `event_image` WRITE;
/*!40000 ALTER TABLE `event_image` DISABLE KEYS */;
INSERT INTO `event_image` VALUES (1,_binary '',0,'https://res.cloudinary.com/dszkninft/image/upload/v1758467104/ruqvrx0dc4xqerpzuaax.png',8);
/*!40000 ALTER TABLE `event_image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `event_place`
--

DROP TABLE IF EXISTS `event_place`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_place` (
  `event_id` int NOT NULL,
  `place_id` int NOT NULL,
  KEY `FKt4lgegyjv5mtkbhw1n4bn6dh9` (`place_id`),
  KEY `FKl7y34u2qtt4f6q43mp2ef71ca` (`event_id`),
  CONSTRAINT `FKl7y34u2qtt4f6q43mp2ef71ca` FOREIGN KEY (`event_id`) REFERENCES `event` (`id`),
  CONSTRAINT `FKt4lgegyjv5mtkbhw1n4bn6dh9` FOREIGN KEY (`place_id`) REFERENCES `place` (`place_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `event_place`
--

LOCK TABLES `event_place` WRITE;
/*!40000 ALTER TABLE `event_place` DISABLE KEYS */;
INSERT INTO `event_place` VALUES (1,1),(2,2),(3,3),(4,4),(5,5),(6,6),(7,7),(8,8);
/*!40000 ALTER TABLE `event_place` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `event_schedule`
--

DROP TABLE IF EXISTS `event_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_schedule` (
  `schedule_id` int NOT NULL AUTO_INCREMENT,
  `activity` varchar(255) DEFAULT NULL,
  `end_time` datetime(6) DEFAULT NULL,
  `start_time` datetime(6) DEFAULT NULL,
  `event_id` int NOT NULL,
  PRIMARY KEY (`schedule_id`),
  KEY `FKrnomdfxthbffuwwkj0oxyqd8h` (`event_id`),
  CONSTRAINT `FKrnomdfxthbffuwwkj0oxyqd8h` FOREIGN KEY (`event_id`) REFERENCES `event` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `event_schedule`
--

LOCK TABLES `event_schedule` WRITE;
/*!40000 ALTER TABLE `event_schedule` DISABLE KEYS */;
INSERT INTO `event_schedule` VALUES (1,'Mở màn','2025-09-28 19:15:00.000000','2025-09-28 19:00:00.000000',1),(2,'Biểu diễn chính','2025-09-28 21:30:00.000000','2025-09-28 19:15:00.000000',1),(3,'Mở màn','2025-09-28 19:15:00.000000','2025-09-28 19:00:00.000000',2),(4,'Biểu diễn chính','2025-09-28 21:30:00.000000','2025-09-28 19:15:00.000000',2),(5,'Mở màn','2025-09-28 19:15:00.000000','2025-09-28 19:00:00.000000',3),(6,'Biểu diễn chính','2025-09-28 21:30:00.000000','2025-09-28 19:15:00.000000',3),(7,'Mở màn','2025-09-28 19:15:00.000000','2025-09-28 19:00:00.000000',4),(8,'Biểu diễn chính','2025-09-28 21:30:00.000000','2025-09-28 19:15:00.000000',4),(9,'Mở màn','2025-09-28 19:15:00.000000','2025-09-28 19:00:00.000000',5),(10,'Biểu diễn chính','2025-09-28 21:30:00.000000','2025-09-28 19:15:00.000000',5),(11,'Mở màn','2025-09-28 19:15:00.000000','2025-09-28 19:00:00.000000',6),(12,'Biểu diễn chính','2025-09-28 21:30:00.000000','2025-09-28 19:15:00.000000',6),(13,'Mở màn','2025-09-28 19:15:00.000000','2025-09-28 19:00:00.000000',7),(14,'Biểu diễn chính','2025-09-28 21:30:00.000000','2025-09-28 19:15:00.000000',7),(15,'Mở màn','2025-09-28 19:15:00.000000','2025-09-28 19:00:00.000000',8),(16,'Biểu diễn chính','2025-09-28 21:30:00.000000','2025-09-28 19:15:00.000000',8);
/*!40000 ALTER TABLE `event_schedule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `event_sequence`
--

DROP TABLE IF EXISTS `event_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_sequence` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `event_sequence`
--

LOCK TABLES `event_sequence` WRITE;
/*!40000 ALTER TABLE `event_sequence` DISABLE KEYS */;
INSERT INTO `event_sequence` VALUES (9);
/*!40000 ALTER TABLE `event_sequence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `event_speaker`
--

DROP TABLE IF EXISTS `event_speaker`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_speaker` (
  `event_id` int NOT NULL,
  `place_id` int NOT NULL,
  `speaker_id` int NOT NULL,
  KEY `FKhlxaio8k4mwjauwdvtw1n5cye` (`place_id`),
  KEY `FKjmtxwwiao8a0t7jdd6bp5r6bk` (`event_id`),
  KEY `FK94k9kfry25ekvjndhncstcci3` (`speaker_id`),
  CONSTRAINT `FK94k9kfry25ekvjndhncstcci3` FOREIGN KEY (`speaker_id`) REFERENCES `speaker` (`speaker_id`),
  CONSTRAINT `FKhlxaio8k4mwjauwdvtw1n5cye` FOREIGN KEY (`place_id`) REFERENCES `speaker` (`speaker_id`),
  CONSTRAINT `FKjmtxwwiao8a0t7jdd6bp5r6bk` FOREIGN KEY (`event_id`) REFERENCES `event` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `event_speaker`
--

LOCK TABLES `event_speaker` WRITE;
/*!40000 ALTER TABLE `event_speaker` DISABLE KEYS */;
/*!40000 ALTER TABLE `event_speaker` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `guest`
--

DROP TABLE IF EXISTS `guest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `guest` (
  `guest_id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`guest_id`),
  UNIQUE KEY `UKl30f0fvs78rfwtjbim6nqo2cp` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `guest`
--

LOCK TABLES `guest` WRITE;
/*!40000 ALTER TABLE `guest` DISABLE KEYS */;
/*!40000 ALTER TABLE `guest` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `guest_ticket`
--

DROP TABLE IF EXISTS `guest_ticket`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `guest_ticket` (
  `guest_ticket_id` int NOT NULL AUTO_INCREMENT,
  `max_quantity` int DEFAULT NULL,
  `quantity_bought` int DEFAULT NULL,
  `guest_id` int NOT NULL,
  `ticket_type_id` int NOT NULL,
  PRIMARY KEY (`guest_ticket_id`),
  UNIQUE KEY `UKdicncstygiglwm4r4069xwul2` (`guest_id`,`ticket_type_id`),
  KEY `FKajo841ddrd961vc8hxb5flxoy` (`ticket_type_id`),
  CONSTRAINT `FK7gmikwur6vrr3t43m5unukdf0` FOREIGN KEY (`guest_id`) REFERENCES `guest` (`guest_id`),
  CONSTRAINT `FKajo841ddrd961vc8hxb5flxoy` FOREIGN KEY (`ticket_type_id`) REFERENCES `ticket_type` (`ticket_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `guest_ticket`
--

LOCK TABLES `guest_ticket` WRITE;
/*!40000 ALTER TABLE `guest_ticket` DISABLE KEYS */;
/*!40000 ALTER TABLE `guest_ticket` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `host`
--

DROP TABLE IF EXISTS `host`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `host` (
  `host_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `organize_id` int DEFAULT NULL,
  `event_id` int NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`host_id`),
  KEY `fk_host_event` (`event_id`),
  KEY `fk_host_user` (`user_id`),
  CONSTRAINT `fk_host_event` FOREIGN KEY (`event_id`) REFERENCES `event` (`id`),
  CONSTRAINT `fk_host_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `host`
--

LOCK TABLES `host` WRITE;
/*!40000 ALTER TABLE `host` DISABLE KEYS */;
/*!40000 ALTER TABLE `host` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `organization`
--

DROP TABLE IF EXISTS `organization`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organization` (
  `org_id` int NOT NULL AUTO_INCREMENT,
  `address` varchar(300) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `org_name` varchar(150) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `website` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `organization`
--

LOCK TABLES `organization` WRITE;
/*!40000 ALTER TABLE `organization` DISABLE KEYS */;
/*!40000 ALTER TABLE `organization` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `place`
--

DROP TABLE IF EXISTS `place`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `place` (
  `place_id` int NOT NULL AUTO_INCREMENT,
  `building` enum('ALPHA','BETA','NONE') NOT NULL,
  `place_name` varchar(150) NOT NULL,
  PRIMARY KEY (`place_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `place`
--

LOCK TABLES `place` WRITE;
/*!40000 ALTER TABLE `place` DISABLE KEYS */;
INSERT INTO `place` VALUES (1,'NONE','Hội trường A'),(2,'NONE','Hội trường A'),(3,'NONE','Hội trường A'),(4,'NONE','Hội trường A'),(5,'NONE','Hội trường A'),(6,'NONE','Hội trường A'),(7,'NONE','Hội trường A'),(8,'NONE','Hội trường A');
/*!40000 ALTER TABLE `place` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `speaker`
--

DROP TABLE IF EXISTS `speaker`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `speaker` (
  `speaker_id` int NOT NULL AUTO_INCREMENT,
  `default_role` enum('ARTIST','MC','OTHER','PERFORMER','SINGER','SPEAKER') NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `profile` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`speaker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `speaker`
--

LOCK TABLES `speaker` WRITE;
/*!40000 ALTER TABLE `speaker` DISABLE KEYS */;
/*!40000 ALTER TABLE `speaker` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ticket_type`
--

DROP TABLE IF EXISTS `ticket_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ticket_type` (
  `ticket_type_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `price` decimal(38,2) DEFAULT NULL,
  `total_quantity` int DEFAULT NULL,
  `event_id` int NOT NULL,
  PRIMARY KEY (`ticket_type_id`),
  KEY `FKaweoyslbbcildghmy091ascje` (`event_id`),
  CONSTRAINT `FKaweoyslbbcildghmy091ascje` FOREIGN KEY (`event_id`) REFERENCES `event` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ticket_type`
--

LOCK TABLES `ticket_type` WRITE;
/*!40000 ALTER TABLE `ticket_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `ticket_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(100) DEFAULT NULL,
  `organization` varchar(150) DEFAULT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `points` int NOT NULL,
  `account_id` int NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UKnrrhhb0bsexvi8ch6wnon9uog` (`account_id`),
  CONSTRAINT `fk_user_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`account_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'phongle@gmail.com','',NULL,0,2);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-09-23 17:48:45
