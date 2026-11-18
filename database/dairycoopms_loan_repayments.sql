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
-- Table structure for table `loan_repayments`
--

DROP TABLE IF EXISTS `loan_repayments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loan_repayments` (
  `id` binary(16) NOT NULL,
  `loan_id` binary(16) NOT NULL,
  `member_id` binary(16) NOT NULL COMMENT 'Denormalised for reporting convenience',
  `repayment_amount` decimal(14,2) NOT NULL,
  `repayment_date` date NOT NULL,
  `period_year` int NOT NULL COMMENT 'Payment period year e.g. 2026',
  `period_month` int NOT NULL COMMENT 'Payment period month 1-12',
  `balance_after` decimal(14,2) NOT NULL COMMENT 'outstanding_balance after this repayment',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PAID' COMMENT 'PAID | REVERSED',
  `notes` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_loan_repayments_period` (`loan_id`,`period_year`,`period_month`) COMMENT '''''''One repayment per loan per month''''''',
  KEY `idx_loan_repayments_loan_id` (`loan_id`),
  KEY `idx_loan_repayments_member_id` (`member_id`),
  KEY `idx_loan_repayments_repayment_date` (`repayment_date`),
  CONSTRAINT `fk_loan_repayments_loan` FOREIGN KEY (`loan_id`) REFERENCES `loans` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_loan_repayments_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Individual monthly loan repayment records per loan';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loan_repayments`
--

LOCK TABLES `loan_repayments` WRITE;
/*!40000 ALTER TABLE `loan_repayments` DISABLE KEYS */;
INSERT INTO `loan_repayments` VALUES (_binary 'õ\'÷\ÃJBÄ§\í÷ƒù\ì&',_binary '£>yò®M>¤¶}\Ì	r\Ñú',_binary '°s›JÛ½ô5\'3°%',128.33,'2026-09-01',2026,9,513.34,'PAID',NULL,'2026-09-11 12:46:14'),(_binary '%lmxQElª\Èû™À»ı',_binary '£>yò®M>¤¶}\Ì	r\Ñú',_binary '°s›JÛ½ô5\'3°%',128.33,'2026-08-01',2026,8,641.67,'PAID',NULL,'2026-07-06 11:08:09'),(_binary 'c†\ÛC\Õp@2¢\Ë]ş$õgE',_binary '2ò$¿)Aµ¿\ÂVûD\ê?',_binary '÷<jFT»®‚\Ú3\Õu',383.33,'2026-08-01',2026,8,766.67,'PAID',NULL,'2026-07-06 11:08:09'),(_binary 'm\Ø\È+U\àMÎš\Ï\ÌÂ¥¶',_binary '9‰MÏ«šGÇ…§ò<{U\Ë',_binary '¨˜\Ğ@0DŞ¼²lu¿\ÄH',287.50,'2026-09-01',2026,9,1150.00,'PAID',NULL,'2026-09-11 12:46:14'),(_binary '/İº\'Kœµ›Ş½y_ÿ',_binary '2ò$¿)Aµ¿\ÂVûD\ê?',_binary '÷<jFT»®‚\Ú3\Õu',383.33,'2026-09-01',2026,9,383.34,'PAID',NULL,'2026-09-11 12:46:14'),(_binary 'µ<¤‡—\êB…bÓŸş,',_binary '9‰MÏ«šGÇ…§ò<{U\Ë',_binary '¨˜\Ğ@0DŞ¼²lu¿\ÄH',287.50,'2026-08-01',2026,8,1437.50,'PAID',NULL,'2026-07-06 11:08:09');
/*!40000 ALTER TABLE `loan_repayments` ENABLE KEYS */;
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
