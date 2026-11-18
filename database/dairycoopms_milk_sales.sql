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
-- Table structure for table `milk_sales`
--

DROP TABLE IF EXISTS `milk_sales`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `milk_sales` (
  `id` binary(16) NOT NULL,
  `invoice_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `processing_company_id` binary(16) DEFAULT NULL,
  `quantity_litres` decimal(12,2) NOT NULL,
  `price_per_litre` decimal(10,4) NOT NULL COMMENT 'Price offered by the processing company',
  `total_revenue` decimal(14,2) NOT NULL COMMENT 'quantity_litres x price_per_litre',
  `sale_date` date NOT NULL,
  `due_date` date DEFAULT NULL,
  `payment_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | RECEIVED',
  `payment_received_at` timestamp NULL DEFAULT NULL COMMENT 'Set by markSalePaymentReceived()',
  `reference_no` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Invoice or purchase order number',
  `notes` text COLLATE utf8mb4_unicode_ci,
  `purchase_order_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `purchase_order_id` binary(16) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sales_invoice_no` (`invoice_no`),
  UNIQUE KEY `uq_milk_sales_reference_no` (`reference_no`),
  KEY `idx_milk_sales_company_id` (`processing_company_id`),
  KEY `idx_milk_sales_sale_date` (`sale_date`),
  KEY `idx_milk_sales_payment_status` (`payment_status`),
  CONSTRAINT `fk_milk_sales_company` FOREIGN KEY (`processing_company_id`) REFERENCES `processing_companies` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Milk sold to processing companies; tracks payment from buyers';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `milk_sales`
--

LOCK TABLES `milk_sales` WRITE;
/*!40000 ALTER TABLE `milk_sales` DISABLE KEYS */;
INSERT INTO `milk_sales` VALUES (_binary '\Ó  ã\ÀA¿ïGU\∆«õ\Ìi',NULL,NULL,150.00,20.0000,3000.00,'2026-07-01',NULL,'RECEIVED','2026-07-02 11:31:39','INQ-2026-0015','Delivery confirmed for PO: PO-MIL-2026-0001 | Company: Milk Productions Zambia',NULL,NULL,'2026-06-22 22:08:04','2026-07-02 11:31:39'),(_binary 'y+≤0òE‡°æQñª\„\'\Â',NULL,NULL,110.00,20.0000,2200.00,'2026-05-04',NULL,'PENDING',NULL,'INQ-2026-0002','Created from inquiry: INQ-2026-0002',NULL,NULL,'2026-05-04 19:26:30','2026-05-04 19:26:30'),(_binary 'N…ØãÅG˚û+\√$f†∂é',NULL,NULL,100.00,25.0000,2500.00,'2026-09-16',NULL,'RECEIVED','2026-09-16 01:20:10','INQ-2026-0022','Delivery confirmed for PO: PO-PAR-2026-0011 | Company: parmalat Zambia Ltd',NULL,NULL,'2026-09-16 01:14:15','2026-09-16 01:20:10'),(_binary 'OF\Ó	\◊F˛îZ\Áõ¸ÚΩ:',NULL,NULL,100.00,25.0000,2500.00,'2026-09-16',NULL,'PENDING',NULL,'INQ-2026-0029','Created from inquiry: INQ-2026-0029',NULL,NULL,'2026-09-16 07:37:46','2026-09-16 07:37:46'),(_binary '\'Ï∫§\ÌK˚ÅgæïXç≠}',NULL,NULL,90.00,20.0000,1800.00,'2026-09-16',NULL,'RECEIVED','2026-09-16 09:38:22','INQ-2026-0019','Delivery confirmed for PO: PO-MIL-2026-0003 | Company: Milk Productions Zambia',NULL,NULL,'2026-09-16 08:49:42','2026-09-16 09:38:22'),(_binary '5@/Ù˚EVüz\ŒàÛ0ê',NULL,NULL,105.00,20.0000,2100.00,'2026-05-17',NULL,'PENDING',NULL,'INQ-2026-0009','Created from inquiry: INQ-2026-0009',NULL,NULL,'2026-05-17 19:15:53','2026-05-17 19:15:53'),(_binary '@wI≠â4GU©	\ \\A˜\Í',NULL,NULL,300.00,20.0000,6000.00,'2026-05-04',NULL,'PENDING',NULL,'INQ-2026-0001','Created from inquiry: INQ-2026-0001',NULL,NULL,'2026-05-04 14:53:34','2026-05-04 14:53:34'),(_binary 'OûÆ®M∏Dòà⁄â\‰xî',NULL,NULL,105.00,20.0000,2100.00,'2026-07-06',NULL,'RECEIVED','2026-07-06 13:38:45','INQ-2026-0021','Delivery confirmed for PO: PO-PRO-2026-0002 | Company: Processing limited PLC zambia',NULL,NULL,'2026-07-06 13:28:01','2026-07-06 13:38:45'),(_binary 'Q¥$\'Du∏WÄ]&LQz',NULL,NULL,105.00,20.0000,2100.00,'2026-06-26',NULL,'PENDING',NULL,'INQ-2026-0016','Delivery confirmed for PO: PO-PAR-2026-0007 | Company: parmalat Zambia Ltd',NULL,NULL,'2026-06-26 11:41:24','2026-06-26 11:41:24'),(_binary 'Wßmkä#M>íY(1—â\–',NULL,NULL,102.00,20.0000,2040.00,'2026-07-08',NULL,'RECEIVED','2026-09-09 12:15:12','INQ-2026-0023','Delivery confirmed for PO: PO-PRO-2026-0003 | Company: Processing limited PLC zambia',NULL,NULL,'2026-07-08 00:30:30','2026-09-09 12:15:12'),(_binary '[\”=\n\ƒsDÕ•\Ë8Æ\–ˆ',NULL,NULL,100.00,20.0000,2000.00,'2026-05-15',NULL,'PENDING',NULL,'INQ-2026-0006','Created from inquiry: INQ-2026-0006',NULL,NULL,'2026-05-15 09:19:07','2026-05-15 09:19:07'),(_binary '_\"\ P\À L¸Å¯˜pbR¶Z',NULL,NULL,130.00,25.0000,3250.00,'2026-09-16',NULL,'RECEIVED','2026-09-16 01:24:28','INQ-2026-0028','Delivery confirmed for PO: PO-PAR-2026-0012 | Company: parmalat Zambia Ltd',NULL,NULL,'2026-09-16 01:22:50','2026-09-16 01:24:28'),(_binary 'j<\÷\√QO˝Ωï˙ø‘Å•',NULL,NULL,102.00,20.0100,2041.02,'2026-05-17',NULL,'PENDING',NULL,'INQ-2026-0007','Created from inquiry: INQ-2026-0007',NULL,NULL,'2026-05-16 22:05:49','2026-05-16 22:05:49'),(_binary 'ußh@Ü∞vR≥\’;\ƒ',NULL,NULL,120.00,20.0000,2400.00,'2026-06-29',NULL,'RECEIVED','2026-06-28 22:57:37','INQ-2026-0014','Delivery confirmed for PO: PO-PRO-2026-0001 | Company: Processing limited PLC zambia',NULL,NULL,'2026-06-22 22:00:57','2026-06-28 22:57:37'),(_binary '}z/ÑÖµ@\ÁÅ3Û\œFYû',NULL,NULL,100.00,25.0000,2500.00,'2026-09-11',NULL,'PENDING',NULL,'INQ-2026-0024','Delivery confirmed for PO: PO-PRO-2026-0004 | Company: Processing limited PLC zambia',NULL,NULL,'2026-09-11 09:50:53','2026-09-11 10:00:01'),(_binary '~¶K¿§´Dºä\‘ıè.Æe',NULL,NULL,104.84,25.0000,2621.00,'2026-09-11',NULL,'RECEIVED','2026-09-11 12:35:01','INQ-2026-0026','Delivery confirmed for PO: PO-MOR-2026-0001 | Company: MORDEN',NULL,NULL,'2026-09-11 12:08:48','2026-09-11 12:35:01'),(_binary '~ª´çßL\rî8\Â)ÒZ\"',NULL,NULL,101.00,20.0000,2020.00,'2026-05-05',NULL,'PENDING',NULL,'INQ-2026-0003','Created from inquiry: INQ-2026-0003',NULL,NULL,'2026-05-04 23:18:52','2026-05-04 23:18:52'),(_binary 'ÖP”ßßdDØ©∏PB\…4å',NULL,NULL,100.00,25.0000,2500.00,'2026-09-11',NULL,'RECEIVED','2026-09-16 00:24:36','INQ-2026-0020','Delivery confirmed for PO: PO-PRO-2026-0005 | Company: Processing limited PLC zambia',NULL,NULL,'2026-07-06 13:28:56','2026-09-16 00:24:36'),(_binary '†&u{UNﬁî\ËÆÉN\◊\Z',NULL,NULL,100.00,20.0000,2000.00,'2026-06-22',NULL,'PENDING',NULL,'INQ-2026-0013','Delivery confirmed for PO: PO-PAR-2026-0006 | Company: parmalat Zambia Ltd',NULL,NULL,'2026-06-22 20:05:22','2026-06-22 20:05:22'),(_binary '£\0fSGú∏>µ™¶X3-',NULL,NULL,110.00,25.0000,2750.00,'2026-09-16',NULL,'RECEIVED','2026-09-16 00:33:59','INQ-2026-0025','Delivery confirmed for PO: PO-PRO-2026-0006 | Company: Processing limited PLC zambia',NULL,NULL,'2026-09-11 11:56:54','2026-09-16 00:33:59'),(_binary '¶ˆåü\‰èGé≤˙_Pë`\Ê\—',NULL,NULL,150.00,25.0000,3750.00,'2026-09-15',NULL,'RECEIVED','2026-09-15 20:47:17','INQ-2026-0027','Delivery confirmed for PO: PO-PAR-2026-0010 | Company: parmalat Zambia Ltd',NULL,NULL,'2026-09-15 20:39:39','2026-09-15 20:47:17'),(_binary '≠xSJö!GUòó≥>W\n≤',NULL,NULL,78.00,20.0000,1560.00,'2026-05-07',NULL,'PENDING',NULL,'INQ-2026-0005','Created from inquiry: INQ-2026-0005',NULL,NULL,'2026-05-07 00:07:02','2026-05-07 00:07:02'),(_binary '≥“ºzë\ÕF;è\‡îgÂ∫¥\¬',NULL,NULL,105.00,20.0000,2100.00,'2026-05-05',NULL,'PENDING',NULL,'INQ-2026-0004','Created from inquiry: INQ-2026-0004',NULL,NULL,'2026-05-05 13:37:13','2026-05-05 13:37:13'),(_binary 'ªó:Ω¶˘K∞∂V6ﬁ§\“\Ÿy',NULL,NULL,100.00,25.0000,2500.00,'2026-09-16',NULL,'PENDING',NULL,'INQ-2026-0030','Created from inquiry: INQ-2026-0030',NULL,NULL,'2026-09-16 12:49:57','2026-09-16 12:49:57'),(_binary '\ﬁT7\ÿ¸˛Eµü+∏WI1§Q',NULL,NULL,150.00,20.0000,3000.00,'2026-06-29',NULL,'RECEIVED','2026-07-01 17:47:28','INQ-2026-0012','Delivery confirmed for PO: PO-PAR-2026-0008 | Company: parmalat Zambia Ltd',NULL,NULL,'2026-06-22 21:38:44','2026-07-01 17:47:28'),(_binary '\‚QÅâ&/HÄÆ…òûn',NULL,NULL,78.00,20.0000,1560.00,'2026-05-17',NULL,'PENDING',NULL,'INQ-2026-0008','Created from inquiry: INQ-2026-0008',NULL,NULL,'2026-05-17 13:17:46','2026-05-17 13:17:46'),(_binary '\Ë\…e\”4lL{´∑coi-\œ',NULL,NULL,80.00,20.0000,1600.00,'2026-07-07',NULL,'RECEIVED','2026-09-15 20:47:44','INQ-2026-0018','Delivery confirmed for PO: PO-MIL-2026-0002 | Company: Milk Productions Zambia',NULL,NULL,'2026-07-01 18:29:17','2026-09-15 20:47:44'),(_binary 'ÏåÉq\r\ÂN¯óò*im[',NULL,NULL,101.00,20.0000,2020.00,'2026-07-01',NULL,'RECEIVED','2026-07-07 19:47:38','INQ-2026-0017','Delivery confirmed for PO: PO-PAR-2026-0009 | Company: parmalat Zambia Ltd',NULL,NULL,'2026-06-28 23:25:13','2026-07-07 19:47:38');
/*!40000 ALTER TABLE `milk_sales` ENABLE KEYS */;
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
