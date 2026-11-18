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
-- Table structure for table `cow_assignments`
--

DROP TABLE IF EXISTS `cow_assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cow_assignments` (
  `id` binary(16) NOT NULL,
  `cow_id` binary(16) NOT NULL,
  `member_id` binary(16) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '1 = currently assigned; 0 = closed',
  `assigned_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `revoked_at` timestamp NULL DEFAULT NULL COMMENT 'Populated by revokeCowAssignment()',
  `revoke_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cow_assignments_cow_id` (`cow_id`),
  KEY `idx_cow_assignments_member_id` (`member_id`),
  KEY `idx_cow_assignments_is_active` (`is_active`),
  KEY `idx_cow_assignments_cow_active` (`cow_id`,`is_active`) COMMENT '''''''existsByCowIdAndActiveTrue''''''',
  KEY `idx_cow_assignments_member_active` (`member_id`,`is_active`) COMMENT '''''''existsByMemberIdAndActiveTrue''''''',
  CONSTRAINT `fk_cow_assignments_cow` FOREIGN KEY (`cow_id`) REFERENCES `cows` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_cow_assignments_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tracks which cooperative cow is assigned to which member';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cow_assignments`
--

LOCK TABLES `cow_assignments` WRITE;
/*!40000 ALTER TABLE `cow_assignments` DISABLE KEYS */;
INSERT INTO `cow_assignments` VALUES (_binary 'UÄ∞/\ÂDzçÇ≤F@êB',_binary 'x\‹JPÒÇ\Ë\ÿ—üH',_binary '˜<èjFTªÆÇ\⁄3è\’u',1,'2026-05-08 11:36:50',NULL,NULL,'2026-05-08 11:36:50'),(_binary '\‘\·w}πH+°d…°\Ó\√¿Û',_binary 'y\¬JPÒÇ\Ë\ÿ—üH',_binary '\«<1sˇM!æ:FÆÖåÛõ',1,'2026-09-15 21:43:37',NULL,NULL,'2026-09-15 21:43:37'),(_binary '\n1JGŸôf%3Oû\–',_binary 'xıJPÒÇ\Ë\ÿ—üH',_binary '®ò\–@0Dﬁº≤luø\ƒH',1,'2026-05-08 11:36:54',NULL,NULL,'2026-05-08 11:36:54'),(_binary '∞û´G#©&?£˚9∏â',_binary 'z\'JPÒÇ\Ë\ÿ—üH',_binary '\«<1sˇM!æ:FÆÖåÛõ',1,'2026-09-15 21:43:56',NULL,NULL,'2026-09-15 21:43:56'),(_binary 'eNQù®Háíì±b\È\Ê«é',_binary 'y`JPÒÇ\Ë\ÿ—üH',_binary '®ò\–@0Dﬁº≤luø\ƒH',1,'2026-05-08 11:37:13',NULL,NULL,'2026-05-08 11:37:13'),(_binary '©\Ëí\\OÑÑ§~j~}.',_binary 'zJPÒÇ\Ë\ÿ—üH',_binary '∞sèõJ€ΩÙ5\'3∞û%',1,'2026-05-08 11:37:47',NULL,NULL,'2026-05-08 11:37:47'),(_binary ')=Û;bE≤¨]h\ﬂs\ÿO0',_binary 'y™JPÒÇ\Ë\ÿ—üH',_binary '∞sèõJ€ΩÙ5\'3∞û%',1,'2026-05-08 11:37:36',NULL,NULL,'2026-05-08 11:37:36'),(_binary '/\–t\‰…ëF\·º	¿∞Ø',_binary 'zYJPÒÇ\Ë\ÿ—üH',_binary '\«<1sˇM!æ:FÆÖåÛõ',1,'2026-09-15 21:44:06',NULL,NULL,'2026-09-15 21:44:06'),(_binary '69¶\‰\◊B	ÄƒÄès\≈O˙',_binary 'y,JPÒÇ\Ë\ÿ—üH',_binary '˜<èjFTªÆÇ\⁄3è\’u',1,'2026-05-08 11:37:18',NULL,NULL,'2026-05-08 11:37:18'),(_binary 'Mmwî\–G¬≥ +2d/ä\È',_binary '\'>9âàMßè˜\ÂsT=\ƒ\…',_binary '˜<èjFTªÆÇ\⁄3è\’u',0,'2026-05-07 10:22:23','2026-05-07 11:08:17','member stopped','2026-05-07 10:22:23'),(_binary 'Pu\‡Œà˝NœûVE√û\ÏB≠',_binary 'yDJPÒÇ\Ë\ÿ—üH',_binary '˜<èjFTªÆÇ\⁄3è\’u',1,'2026-05-08 11:37:07',NULL,NULL,'2026-05-08 11:37:07'),(_binary 'R|\∆dAJ)≥˝µªåàÆ\œ',_binary 'yxJPÒÇ\Ë\ÿ—üH',_binary '®ò\–@0Dﬁº≤luø\ƒH',1,'2026-05-08 11:37:23',NULL,NULL,'2026-05-08 11:37:23'),(_binary 'SÛ»Ñ$\ZL˚â}M•<iv',_binary 'yJPÒÇ\Ë\ÿ—üH',_binary '®ò\–@0Dﬁº≤luø\ƒH',1,'2026-05-08 11:36:57',NULL,NULL,'2026-05-08 11:36:57'),(_binary '¢\0{™ÙZGPº\◊\◊lK\Ÿd[',_binary 'yíJPÒÇ\Ë\ÿ—üH',_binary '∞sèõJ€ΩÙ5\'3∞û%',1,'2026-05-08 11:37:29',NULL,NULL,'2026-05-08 11:37:29'),(_binary '¥\–@ôw\„Cë\≈=DF$∂\À',_binary '\'>9âàMßè˜\ÂsT=\ƒ\…',_binary '®ò\–@0Dﬁº≤luø\ƒH',1,'2026-05-07 11:11:09',NULL,NULL,'2026-05-07 11:11:09'),(_binary '\ÈPº%\ Aæ©7n˜Ql˚',_binary 'y˜JPÒÇ\Ë\ÿ—üH',_binary '∞sèõJ€ΩÙ5\'3∞û%',1,'2026-05-08 11:36:39',NULL,NULL,'2026-05-08 11:36:39'),(_binary '\Ï\Â∞\–ÆCHÅä©\Ïe%nj',_binary 'x∂JPÒÇ\Ë\ÿ—üH',_binary '˜<èjFTªÆÇ\⁄3è\’u',1,'2026-05-08 11:36:46',NULL,NULL,'2026-05-08 11:36:46');
/*!40000 ALTER TABLE `cow_assignments` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-18 13:31:07
