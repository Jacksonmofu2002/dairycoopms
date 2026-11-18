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
-- Table structure for table `milk_prices`
--

DROP TABLE IF EXISTS `milk_prices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `milk_prices` (
  `id` binary(16) NOT NULL,
  `price_per_litre` decimal(10,4) NOT NULL COMMENT 'Cooperative purchase price ZMW per litre',
  `effective_date` date NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '1 = current price; 0 = historical',
  `set_by` binary(16) DEFAULT NULL COMMENT 'FK to users â€“ admin who set this price',
  `notes` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_milk_prices_is_active` (`is_active`),
  KEY `idx_milk_prices_effective_date` (`effective_date`),
  KEY `idx_milk_prices_set_by` (`set_by`),
  CONSTRAINT `fk_milk_prices_set_by` FOREIGN KEY (`set_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Cooperative milk price per litre; one active price at a time';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `milk_prices`
--

LOCK TABLES `milk_prices` WRITE;
/*!40000 ALTER TABLE `milk_prices` DISABLE KEYS */;
INSERT INTO `milk_prices` VALUES (_binary 'l=ôDNI$¢~\áŸÁZX',25.0000,'2026-05-04',1,_binary '¨Ge±F\îñ®\Ä\è\ØÑŸH','new sale price','2026-05-04 12:22:42');
/*!40000 ALTER TABLE `milk_prices` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-18 13:31:06
