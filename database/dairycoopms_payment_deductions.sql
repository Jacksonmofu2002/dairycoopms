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
-- Table structure for table `payment_deductions`
--

DROP TABLE IF EXISTS `payment_deductions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_deductions` (
  `id` binary(16) NOT NULL,
  `member_id` binary(16) NOT NULL,
  `period_year` int NOT NULL COMMENT 'Payment period year',
  `period_month` int NOT NULL COMMENT 'Payment period month 1-12',
  `deduction_type_id` int DEFAULT NULL COMMENT 'FK to deduction_types; NULL for loan repayments',
  `loan_id` binary(16) DEFAULT NULL COMMENT 'FK to loans; NULL for cooperative deductions',
  `amount` decimal(14,2) NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Human-readable label shown on the payslip',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_payment_deductions_member_id` (`member_id`),
  KEY `idx_payment_deductions_period` (`period_year`,`period_month`),
  KEY `idx_payment_deductions_member_period` (`member_id`,`period_year`,`period_month`) COMMENT '''''''Idempotency + pre-payment check''''''',
  KEY `idx_payment_deductions_type_id` (`deduction_type_id`),
  KEY `idx_payment_deductions_loan_id` (`loan_id`),
  CONSTRAINT `fk_payment_deductions_loan` FOREIGN KEY (`loan_id`) REFERENCES `loans` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_payment_deductions_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_payment_deductions_type` FOREIGN KEY (`deduction_type_id`) REFERENCES `deduction_types` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Calculated monthly deductions per member; must exist before payment is processed';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_deductions`
--

LOCK TABLES `payment_deductions` WRITE;
/*!40000 ALTER TABLE `payment_deductions` DISABLE KEYS */;
INSERT INTO `payment_deductions` VALUES (_binary '>s|\é0HH¸\ì»@`¡Á',_binary '¨˜\Ğ@0DŞ¼²lu¿\ÄH',2026,5,5,NULL,865.00,'Cow Maintenance funds','2026-05-11 14:03:01'),(_binary 'er½ô.ADeƒ!iR| ¤',_binary '°s›JÛ½ô5\'3°%',2026,9,NULL,_binary '£>yò®M>¤¶}\Ì	r\Ñú',128.33,'Loan repayment â€“ a33e79f2','2026-09-11 12:46:14'),(_binary '}¨±œ\ÌL–’ÜºQ4X\àú',_binary '°s›JÛ½ô5\'3°%',2026,5,5,NULL,1069.95,'Cow Maintenance funds','2026-05-11 14:03:01'),(_binary '„=\Ö\ÎdkB!š~¾«­Q/',_binary '÷<jFT»®‚\Ú3\Õu',2026,8,NULL,_binary '2ò$¿)Aµ¿\ÂVûD\ê?',383.33,'Loan repayment â€“ 32f2248e','2026-07-06 11:08:09'),(_binary '¢\Ü\Ş.¶AS“ƒ\Ïu–0',_binary '¨˜\Ğ@0DŞ¼²lu¿\ÄH',2026,8,NULL,_binary '9‰MÏ«šGÇ…§ò<{U\Ë',287.50,'Loan repayment â€“ 39894dcf','2026-07-06 11:08:09'),(_binary '\Çù6œ^bM±ŒóøRD@¡',_binary '÷<jFT»®‚\Ú3\Õu',2026,9,NULL,_binary '2ò$¿)Aµ¿\ÂVûD\ê?',383.33,'Loan repayment â€“ 32f2248e','2026-09-11 12:46:14'),(_binary '\äD£¸G*—S–}\ã\â\Ñ',_binary '°s›JÛ½ô5\'3°%',2026,8,NULL,_binary '£>yò®M>¤¶}\Ì	r\Ñú',128.33,'Loan repayment â€“ a33e79f2','2026-07-06 11:08:09'),(_binary 'õ8ú yKŸ#6šB¨Bö',_binary '¨˜\Ğ@0DŞ¼²lu¿\ÄH',2026,9,NULL,_binary '9‰MÏ«šGÇ…§ò<{U\Ë',287.50,'Loan repayment â€“ 39894dcf','2026-09-11 12:46:14'),(_binary 'úš„N2=CC„k\\±\Ò)',_binary '÷<jFT»®‚\Ú3\Õu',2026,5,5,NULL,852.45,'Cow Maintenance funds','2026-05-11 14:03:01');
/*!40000 ALTER TABLE `payment_deductions` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-18 13:31:05
