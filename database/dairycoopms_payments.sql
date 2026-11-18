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
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` binary(16) NOT NULL,
  `member_id` binary(16) NOT NULL,
  `period_year` int NOT NULL COMMENT 'e.g. 2026',
  `period_month` int NOT NULL COMMENT '1-12',
  `gross_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT 'Total earnings before deductions',
  `total_deductions` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT 'Sum of all payment_deductions rows',
  `net_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT 'gross_amount minus total_deductions ‚Äì minimum 0',
  `payment_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID | PAID',
  `paid_at` timestamp NULL DEFAULT NULL COMMENT 'Set by markAsPaid()',
  `payment_method` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'CASH | MOBILE_MONEY | BANK_TRANSFER',
  `reference_no` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `processed_by` binary(16) DEFAULT NULL COMMENT 'FK to users ‚Äì accountant who processed this',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_payments_member_period` (`member_id`,`period_year`,`period_month`) COMMENT '''''''One payment record per member per month''''''',
  KEY `idx_payments_member_id` (`member_id`),
  KEY `idx_payments_period` (`period_year`,`period_month`),
  KEY `idx_payments_payment_status` (`payment_status`),
  KEY `idx_payments_processed_by` (`processed_by`),
  KEY `idx_payments_member_status` (`member_id`,`payment_status`),
  CONSTRAINT `fk_payments_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_payments_processed_by` FOREIGN KEY (`processed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Monthly member payments; UNPAID until accountant marks as PAID';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payments`
--

LOCK TABLES `payments` WRITE;
/*!40000 ALTER TABLE `payments` DISABLE KEYS */;
INSERT INTO `payments` VALUES (_binary 'Û\‡DE›≠∂ms5]9Y',_binary '∞sèõJ€ΩÙ5\'3∞û%',2026,8,3000.00,128.33,2871.67,'PAID','2026-07-06 14:01:11','Bank Transfer','MKP-2026-0007',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-07-06 11:08:09','2026-07-06 14:01:11'),(_binary '-\nFêûH8ë\Ô\‹r|PÇ',_binary '∞sèõJ€ΩÙ5\'3∞û%',2026,5,10699.50,1069.95,9629.55,'PAID','2026-07-05 20:06:04','Bank Transfer','MKP-2026-0005',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-05-11 14:03:01','2026-07-05 20:06:04'),(_binary '1Ñ∏V\0Oeáèõ\‘˛$_∂',_binary '®ò\–@0Dﬁº≤luø\ƒH',2026,9,3000.00,287.50,2712.50,'UNPAID',NULL,NULL,NULL,_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-09-11 12:46:14','2026-09-11 12:46:14'),(_binary '11Ü‡°àFÑé*•\ÀJtÇ',_binary '˜<èjFTªÆÇ\⁄3è\’u',2026,5,8524.50,852.45,7672.05,'PAID','2026-07-05 20:07:25','Bank Transfer','MKP-2026-0006',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-05-11 14:03:01','2026-07-05 20:07:25'),(_binary '5£ì∫\ÁJâòEˆBAì]n',_binary '∞sèõJ€ΩÙ5\'3∞û%',2026,4,4674.75,0.00,4674.75,'PAID','2026-05-13 00:16:59','Bank Transfer','MKP-2026-0001',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-05-04 18:06:36','2026-05-13 00:16:59'),(_binary ';ö\Ó†n\È@@ø•√≥wNê',_binary '∞sèõJ€ΩÙ5\'3∞û%',2026,9,3000.00,128.33,2871.67,'PAID','2026-09-11 12:51:32','Airtel Money','MKP-2026-0010',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-09-11 12:46:14','2026-09-11 12:51:32'),(_binary 'CoS´=EMò\Î€ö\≈\ƒˇc',_binary '®ò\–@0Dﬁº≤luø\ƒH',2026,8,3000.00,287.50,2712.50,'PAID','2026-07-06 14:01:31','Bank Transfer','MKP-2026-0008',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-07-06 11:08:09','2026-07-06 14:01:31'),(_binary 'G…äå\’CEá∏F\“ÎäΩp',_binary '®ò\–@0Dﬁº≤luø\ƒH',2026,5,8650.00,865.00,7785.00,'PAID','2026-05-13 00:33:16','Bank Transfer','MKP-2026-0004',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-05-11 14:03:01','2026-05-13 00:33:16'),(_binary 'V≠´ô\…\\J§å[¢L∏ê\rX',_binary '®ò\–@0Dﬁº≤luø\ƒH',2026,4,3825.00,0.00,3825.00,'PAID','2026-05-13 00:17:56','Bank Transfer','MKP-2026-0003',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-05-04 18:06:36','2026-05-13 00:17:56'),(_binary 'mà˚ö\≈UAi¨\÷ RSK',_binary '˜<èjFTªÆÇ\⁄3è\’u',2026,9,3000.00,383.33,2616.67,'PAID','2026-09-11 12:52:16','Airtel Money','MKP-2026-0011',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-09-11 12:46:14','2026-09-11 12:52:16'),(_binary 'òbc\n≤MB…ñ`y7õ6ë0',_binary '˜<èjFTªÆÇ\⁄3è\’u',2026,8,3000.00,383.33,2616.67,'PAID','2026-07-06 14:01:47','Bank Transfer','MKP-2026-0009',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-07-06 11:08:09','2026-07-06 14:01:47'),(_binary 'ıúj]√¢J6≤\Ô)úpõ6',_binary '˜<èjFTªÆÇ\⁄3è\’u',2026,4,4499.50,0.00,4499.50,'PAID','2026-05-13 00:17:33','Bank Transfer','MKP-2026-0002',_binary '\ÿ%∑)B.GløÚ]ÂüêB7','2026-05-04 18:06:36','2026-05-13 00:17:33');
/*!40000 ALTER TABLE `payments` ENABLE KEYS */;
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
