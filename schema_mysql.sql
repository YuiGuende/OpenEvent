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
INSERT INTO `event` VALUES ('Event',1,'Thưởng thức âm nhạc, giao lưu nghệ sĩ.','2025-09-17 16:44:34.783519','Đêm nhạc đặc biệt cùng các nghệ sĩ nổi tiếng.','2025-09-28 22:00:00.000000','2025-09-25 23:59:59.000000','https://example.com/music.jpg','Trải nghiệm sân khấu chuyên nghiệp',50,'2025-09-20 10:00:00.000000','2025-09-28 19:00:00.000000','DRAFT','Live Music Night',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `event` ENABLE KEYS */;
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
INSERT INTO `event_place` VALUES (1,1);
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `event_schedule`
--

LOCK TABLES `event_schedule` WRITE;
/*!40000 ALTER TABLE `event_schedule` DISABLE KEYS */;
INSERT INTO `event_schedule` VALUES (1,'Mở màn','2025-09-28 19:15:00.000000','2025-09-28 19:00:00.000000',1),(2,'Biểu diễn chính','2025-09-28 21:30:00.000000','2025-09-28 19:15:00.000000',1);
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
INSERT INTO `event_sequence` VALUES (2);
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
  KEY `FKhlxaio8k4mwjauwdvtw1n5cye` (`place_id`),
  KEY `FKjmtxwwiao8a0t7jdd6bp5r6bk` (`event_id`),
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `place`
--

LOCK TABLES `place` WRITE;
/*!40000 ALTER TABLE `place` DISABLE KEYS */;
INSERT INTO `place` VALUES (1,'NONE','Hội trường A');
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
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-09-17 16:53:21
