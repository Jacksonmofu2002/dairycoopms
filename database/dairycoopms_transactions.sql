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
-- Table structure for table `transactions`
--

DROP TABLE IF EXISTS `transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transactions` (
  `id` binary(16) NOT NULL,
  `transaction_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MILK_PAYMENT | LOAN_DISBURSEMENT | LOAN_REPAYMENT | MILK_SALE_INCOME | DEDUCTION',
  `reference_id` binary(16) DEFAULT NULL COMMENT 'Source record: payment_id, loan_id, milk_sale_id etc.',
  `member_id` binary(16) DEFAULT NULL COMMENT 'NULL for company-level entries',
  `amount` decimal(14,2) NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `created_by` binary(16) DEFAULT NULL COMMENT 'FK to users',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `metadata` json DEFAULT NULL COMMENT 'Extra data for reporting',
  PRIMARY KEY (`id`),
  KEY `idx_transactions_transaction_type` (`transaction_type`),
  KEY `idx_transactions_member_id` (`member_id`),
  KEY `idx_transactions_reference_id` (`reference_id`),
  KEY `idx_transactions_created_at` (`created_at`),
  KEY `fk_transactions_created_by` (`created_by`),
  CONSTRAINT `fk_transactions_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_transactions_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Financial ledger ‚Äì one row per monetary event';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transactions`
--

LOCK TABLES `transactions` WRITE;
/*!40000 ALTER TABLE `transactions` DISABLE KEYS */;
INSERT INTO `transactions` VALUES (_binary '±\ÈI—òOI¢ÜÀ¢¬±','LOAN_REPAYMENT',_binary '9âMœ´öG«ÖßÚ<{U\À',_binary '®ò\–@0Dﬁº≤luø\ƒH',287.50,'Loan repayment 2026/9',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-09-11 12:46:14',NULL),(_binary '=\Ï%ïáEE–∫©#Y/T\◊˛','LOAN_REPAYMENT',_binary '£>yÚêÆM>§∂}\Ã	r\—˙',_binary '∞sèõJ€ΩÙ5\'3∞û%',128.33,'Loan repayment 2026/9',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-09-11 12:46:14',NULL),(_binary 'GV^8∂Máßà0`;˘2','DEDUCTION',_binary '\'§\Îh§I|∑†¯k†\Á∂',_binary '∞sèõJ€ΩÙ5\'3∞û%',1069.95,'Cow Maintenance funds deduction 2026/5',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-05-11 14:03:01',NULL),(_binary 'I\⁄3E“ÄKÙæ<ÛMI\Ê¿\'','LOAN_REPAYMENT',_binary '2Ú$éø)Aµø\¬V˚D\Í?',_binary '˜<èjFTªÆÇ\⁄3è\’u',383.33,'Loan repayment 2026/8',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-07-06 11:08:09',NULL),(_binary 'N©>\¬˚mN%£üI¢¶&9','LOAN_REPAYMENT',_binary '£>yÚêÆM>§∂}\Ã	r\—˙',_binary '∞sèõJ€ΩÙ5\'3∞û%',128.33,'Loan repayment 2026/8',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-07-06 11:08:09',NULL),(_binary 'q\ÿ|ñ@K\Ìë2∂˜W*ú™','DEDUCTION',_binary ':¨\◊:.DM¢≠\ÌYØ\€k',_binary '®ò\–@0Dﬁº≤luø\ƒH',865.00,'Cow Maintenance funds deduction 2026/5',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-05-11 14:03:01',NULL),(_binary '¢d´w`¡Kˇé˙ö%Æ\›\Ì','LOAN_REPAYMENT',_binary '2Ú$éø)Aµø\¬V˚D\Í?',_binary '˜<èjFTªÆÇ\⁄3è\’u',383.33,'Loan repayment 2026/9',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-09-11 12:46:14',NULL),(_binary 'π1|\‚BıN ±WπHxÉ','LOAN_REPAYMENT',_binary '9âMœ´öG«ÖßÚ<{U\À',_binary '®ò\–@0Dﬁº≤luø\ƒH',287.50,'Loan repayment 2026/8',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-07-06 11:08:09',NULL),(_binary '\„W≤5¸Cè0wRRÑ\÷g','DEDUCTION',_binary '∂F°*ıAœé(F\‹v\0î',_binary '˜<èjFTªÆÇ\⁄3è\’u',852.45,'Cow Maintenance funds deduction 2026/5',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-05-11 14:03:01',NULL);
/*!40000 ALTER TABLE `transactions` ENABLE KEYS */;
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
