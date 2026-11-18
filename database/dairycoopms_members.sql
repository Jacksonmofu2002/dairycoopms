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
-- Table structure for table `members`
--

DROP TABLE IF EXISTS `members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `members` (
  `id` binary(16) NOT NULL,
  `member_no` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Auto-generated in service: MEM-00001',
  `full_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nrc` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'National Registration Card ‚Äì unique identifier',
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `bank_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Bank name',
  `account_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Account holder name',
  `account_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Bank account number',
  `branch_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Branch name/code',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | APPROVED | REJECTED',
  `rejection_reason` text COLLATE utf8mb4_unicode_ci,
  `notification_read` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0 = unread notification from admin (approve/reject action)',
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0 = deactivated; blocks deliveries and assignments',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_members_member_no` (`member_no`),
  UNIQUE KEY `uq_members_nrc` (`nrc`),
  UNIQUE KEY `uq_members_email` (`email`),
  KEY `idx_members_status` (`status`),
  KEY `idx_members_is_active` (`is_active`),
  KEY `idx_members_phone` (`phone`),
  KEY `idx_members_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Cooperative farmer records';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `members`
--

LOCK TABLES `members` WRITE;
/*!40000 ALTER TABLE `members` DISABLE KEYS */;
INSERT INTO `members` VALUES (_binary '∞sèõJ€ΩÙ5\'3∞û%','MEM-00003','GIFT NANSUNGA','312230/10/1','gnansunga96@gmail.com','0777332280','31629, LUSAKA MATERO AREA','INDO ZAMBIA BANK','GIFT NANSUNGA','7507070100107','LSK-TXT-00-MN','APPROVED',NULL,1,1,'2026-05-04 13:52:21','2026-05-04 13:56:14'),(_binary '˜<èjFTªÆÇ\⁄3è\’u','MEM-00001','NAOMI KAFWIMBI','183364/48/1','naomikafwimbi50@gmail.com','+260976515732','31629, KASAMA MULENGA HILLS  AREA','ZANACO','NAOMI KAFWIMBI','7117060100109','KSM-MMTXT-00','APPROVED',NULL,1,1,'2026-05-04 13:38:19','2026-05-07 10:23:23'),(_binary '®ò\–@0Dﬁº≤luø\ƒH','MEM-00002','LOVEMORE CHEELE','202506/10/1','lovemorecheele@gmail.com','0977332290','31629, LUSAKA INDUSTRIAL AREA','FNB','LOVERMORE CHEELE','8507070100222','KT-TXT-000-MN','APPROVED',NULL,1,1,'2026-05-04 13:50:13','2026-05-04 14:03:10'),(_binary '\«<1sˇM!æ:FÆÖåÛõ','MEM-00004','AARON CHISHIMBA','204289/48/1','chishimbaaaron0@gmail.com','0979805348','kitwe','ZANACO','CHISHIMBA AARON','00099999222','KT-TXT-020-MN','APPROVED',NULL,1,1,'2026-09-15 10:48:37','2026-09-15 21:21:58');
/*!40000 ALTER TABLE `members` ENABLE KEYS */;
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
