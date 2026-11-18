-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: dairycoopms
-- ------------------------------------------------------
-- Server version	8.0.45

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
-- Table structure for table `processing_companies`
--

DROP TABLE IF EXISTS `processing_companies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `processing_companies` (
  `id` binary(16) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contact_person` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | INACTIVE',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_processing_companies_name` (`name`),
  KEY `idx_processing_companies_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='External milk buyers';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `processing_companies`
--

LOCK TABLES `processing_companies` WRITE;
/*!40000 ALTER TABLE `processing_companies` DISABLE KEYS */;
INSERT INTO `processing_companies` VALUES (_binary '\nm«œú@\è·\×s€_œQ','Milk Productions Zambia','Siwale PW','0979988876','milkproductions@gmail.com','Plot No. 0099 Silverest Lusaka Zambia','ACTIVE','2026-06-22 20:47:38','2026-09-09 11:28:28'),(_binary 'qEO±\ÔCD±Dy¦’\Ú','parmalat Zambia Ltd','Divine Kabanda','+260976515732','divine@parmalatzambia.com','31629, LUSAKA INDUSTRIAL AREA','ACTIVE','2026-05-04 13:03:06','2026-06-22 20:11:48'),(_binary '€\ÍöZA›;\Õ\ç½;','Processing limited PLC zambia','Silanda J. Mofu','0769312785','processinglimited@gmail.com','Chamba Valley, Plot No.2311, Lusaka Zambia','ACTIVE','2026-06-22 20:33:30','2026-09-09 11:27:03'),(_binary '÷O\Ó(\ç\ÎFº\nû·ª°º','MORDEN','GERSHION CHOMBA','0767077387','gershomchomba@gmail.com','lusaka, northmead','ACTIVE','2026-09-09 11:41:28','2026-09-09 11:41:28');
/*!40000 ALTER TABLE `processing_companies` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-18 13:31:08
