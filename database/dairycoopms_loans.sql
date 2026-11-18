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
-- Table structure for table `loans`
--

DROP TABLE IF EXISTS `loans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loans` (
  `id` binary(16) NOT NULL,
  `member_id` binary(16) NOT NULL,
  `loan_amount` decimal(14,2) NOT NULL COMMENT 'Original loan amount',
  `outstanding_balance` decimal(14,2) NOT NULL COMMENT 'Remaining balance; decremented each month',
  `term_months` int NOT NULL COMMENT 'Repayment period in months ‚Äì between 1 and 60',
  `monthly_instalment` decimal(14,2) NOT NULL COMMENT 'loan_amount / term_months',
  `interest_rate` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT 'Annual interest %; 0 = interest-free',
  `purpose` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | APPROVED | REJECTED',
  `rejection_reason` text COLLATE utf8mb4_unicode_ci,
  `credit_score` tinyint DEFAULT NULL COMMENT 'Score 1-10 from embedded scoring engine',
  `score_decision` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'AUTO_APPROVED | AUTO_REJECTED | MANUAL',
  `score_reason` text COLLATE utf8mb4_unicode_ci,
  `score_qualifying_days` tinyint DEFAULT NULL COMMENT 'Days with 50+ litres in scoring period',
  `score_total_litres` decimal(10,2) DEFAULT NULL COMMENT 'Total litres delivered in scoring period',
  `approved_by` binary(16) DEFAULT NULL COMMENT 'FK to users ‚Äì must have LOAN_OFFICER role',
  `approved_date` date DEFAULT NULL,
  `request_date` date NOT NULL,
  `start_date` date DEFAULT NULL,
  `maturity_date` date DEFAULT NULL COMMENT 'start_date + term_months',
  `disbursement_status` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'NULL | PENDING_DISBURSEMENT | AWAITING_FUNDS | DISBURSED',
  `disbursed_at` timestamp NULL DEFAULT NULL COMMENT 'When the loan was disbursed to member',
  `disbursed_by` binary(16) DEFAULT NULL COMMENT 'FK to users - accountant who processed disbursement',
  `disbursement_reference` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Bank transfer reference / voucher number',
  `disbursement_notes` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_loans_member_id` (`member_id`),
  KEY `idx_loans_status` (`status`),
  KEY `idx_loans_approved_by` (`approved_by`),
  KEY `idx_loans_request_date` (`request_date`),
  KEY `idx_loans_member_status` (`member_id`,`status`) COMMENT '''''''findByMemberIdAndStatus queries''''''',
  KEY `fk_loan_disbursed_by` (`disbursed_by`),
  CONSTRAINT `fk_loan_disbursed_by` FOREIGN KEY (`disbursed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_loans_approved_by` FOREIGN KEY (`approved_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_loans_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Member loan records; repayments deducted monthly before payment';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loans`
--

LOCK TABLES `loans` WRITE;
/*!40000 ALTER TABLE `loans` DISABLE KEYS */;
INSERT INTO `loans` VALUES (_binary 'RÛÄ\√\¬Lüµ(^∏ZU–≠',_binary '˜<èjFTªÆÇ\⁄3è\’u',1000.00,1000.00,3,383.33,15.00,'for home use','REJECTED','Not approved. Best month (May 2026): only 4 qualifying day(s) with 50+ litres.\r\nYou need at least 5 days in a month. Score: 6/10.\r\nDeliver more and reapply.',6,'AUTO_REJECTED','Not approved. Best month (May 2026): only 4 qualifying day(s) with 50+ litres.\r\nYou need at least 5 days in a month. Score: 6/10.\r\nDeliver more and reapply.',4,340.98,NULL,NULL,'2026-05-07',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-05-07 10:28:00','2026-05-07 10:28:00'),(_binary '2Ú$éø)Aµø\¬V˚D\Í?',_binary '˜<èjFTªÆÇ\⁄3è\’u',1000.00,383.34,3,383.33,15.00,'HOME USE ','APPROVED',NULL,8,'AUTO_APPROVED','Approved based on your delivery record in May 2026.\r\n8 qualifying days with 50+ litres (required: 5).\r\nScore: 8/10.',8,797.94,NULL,'2026-06-19','2026-06-19','2026-07-01','2026-10-01','DISBURSED','2026-09-15 21:34:17',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','DIS-2026-0003','','2026-06-19 09:40:31','2026-09-15 21:34:17'),(_binary '9âMœ´öG«ÖßÚ<{U\À',_binary '®ò\–@0Dﬁº≤luø\ƒH',1500.00,1150.00,6,287.50,15.00,'for home use','APPROVED',NULL,7,'AUTO_APPROVED','Approved based on your delivery record in May 2026.\r\n6 qualifying days with 50+ litres (required: 5).\r\nScore: 7/10.',6,481.98,NULL,'2026-05-11','2026-05-11','2026-06-01','2026-12-01','DISBURSED','2026-05-13 00:32:28',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','DIS-2026-0001','','2026-05-11 21:20:10','2026-09-11 12:46:14'),(_binary '?tò˝\»@{û˘\Ï_ÙOÒg',_binary '\«<1sˇM!æ:FÆÖåÛõ',3000.00,3900.00,6,650.00,30.00,'home use/personal use ','APPROVED',NULL,8,'AUTO_APPROVED','Approved based on your delivery record in September 2026.\r\n7 qualifying days with 50+ litres (required: 5).\r\nScore: 8/10.',7,792.00,NULL,'2026-09-15','2026-09-15','2026-10-01','2027-04-01','PENDING_DISBURSEMENT',NULL,NULL,NULL,NULL,'2026-09-15 21:20:49','2026-09-15 21:20:49'),(_binary '£>yÚêÆM>§∂}\Ã	r\—˙',_binary '∞sèõJ€ΩÙ5\'3∞û%',700.00,513.34,6,128.33,10.00,'HOME USE','APPROVED',NULL,8,'AUTO_APPROVED','Approved based on your delivery record in May 2026.\r\n7 qualifying days with 50+ litres (required: 5).\r\nScore: 8/10.',7,643.96,NULL,'2026-05-13','2026-05-13','2026-06-01','2026-12-01','DISBURSED','2026-05-19 12:24:24',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','DIS-2026-0002','','2026-05-13 00:00:04','2026-09-11 12:46:14');
/*!40000 ALTER TABLE `loans` ENABLE KEYS */;
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
