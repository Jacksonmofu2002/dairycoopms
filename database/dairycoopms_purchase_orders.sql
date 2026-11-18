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
-- Table structure for table `purchase_orders`
--

DROP TABLE IF EXISTS `purchase_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_orders` (
  `id` binary(16) NOT NULL,
  `po_number` varchar(50) NOT NULL,
  `inquiry_id` binary(16) DEFAULT NULL,
  `company_name` varchar(255) NOT NULL,
  `contact_person` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `phone` varchar(50) NOT NULL,
  `po_date` date NOT NULL,
  `quantity_litres` decimal(14,2) NOT NULL,
  `unit_price` decimal(14,2) NOT NULL,
  `total_amount` decimal(14,2) NOT NULL,
  `delivery_date` date NOT NULL,
  `delivery_address` text,
  `special_instructions` text,
  `payment_terms` varchar(100) DEFAULT 'Net 30 days',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `approved_by` binary(16) DEFAULT NULL,
  `approved_at` timestamp NULL DEFAULT NULL,
  `rejection_reason` text,
  `rejected_at` timestamp NULL DEFAULT NULL,
  `order_id` binary(16) DEFAULT NULL,
  `invoice_no` varchar(50) DEFAULT NULL,
  `invoiced_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `submitted_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `po_number` (`po_number`),
  KEY `approved_by` (`approved_by`),
  KEY `order_id` (`order_id`),
  KEY `idx_purchase_orders_inquiry` (`inquiry_id`),
  KEY `idx_purchase_orders_status` (`status`),
  KEY `idx_purchase_orders_email` (`email`),
  KEY `idx_purchase_orders_company` (`company_name`),
  KEY `idx_purchase_orders_po_number` (`po_number`),
  KEY `idx_purchase_orders_submitted_at` (`submitted_at`),
  KEY `idx_purchase_orders_approved_at` (`approved_at`),
  CONSTRAINT `purchase_orders_ibfk_1` FOREIGN KEY (`inquiry_id`) REFERENCES `milk_inquiries` (`id`),
  CONSTRAINT `purchase_orders_ibfk_2` FOREIGN KEY (`approved_by`) REFERENCES `users` (`id`),
  CONSTRAINT `purchase_orders_ibfk_3` FOREIGN KEY (`order_id`) REFERENCES `milk_sales` (`id`),
  CONSTRAINT `check_po_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'SUBMITTED',_utf8mb4'APPROVED',_utf8mb4'REJECTED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_orders`
--

LOCK TABLES `purchase_orders` WRITE;
/*!40000 ALTER TABLE `purchase_orders` DISABLE KEYS */;
INSERT INTO `purchase_orders` VALUES (_binary 'µ¢\0\ËE\Óê\—¿JjR','PO-PAR-2026-0004',_binary '\Ì{•X{\·OZ∞\ËπiUe\„','parmalat Zambia Ltd','Divine Kabanda','divine@parmalatzambia.com','+260976515732','2026-05-17',105.00,20.00,2100.00,'2026-05-22','',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-05-17 20:24:05',NULL,NULL,NULL,NULL,NULL,'2026-05-17 19:16:22','2026-05-17 20:24:05','2026-05-17 20:10:54'),(_binary 'i\Ë∫‘¶Bòπ\Õ&Ω≠\Ô∂j','PO-PRO-2026-0003',_binary 'nπ\ƒ.ëHºµ\Ê\◊√âÜ\›','Processing limited PLC zambia','Silanda J. Mofu','processinglimited@gmail.com','0769312785','2026-07-08',102.00,20.00,2040.00,'2026-07-10','Lusaka city industrial area ',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-07-08 00:33:00',NULL,NULL,NULL,NULL,NULL,'2026-07-08 00:31:45','2026-07-08 00:33:00','2026-07-08 00:31:53'),(_binary '§k\'ﬂµL©\Ó\«\—?\Z)','PO-PAR-2026-0013',_binary '\÷RLÜcF—åLïJì£bá','parmalat Zambia Ltd','Divine Kabanda','divine@parmalatzambia.com','+260976515732','2026-09-16',100.00,25.00,2500.00,'2026-09-18','lusaka warehouse plot 222000',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-09-16 12:50:43',NULL,NULL,NULL,NULL,NULL,'2026-09-16 12:50:16','2026-09-16 12:50:43','2026-09-16 12:50:22'),(_binary '\Zy(Pö¡G÷¥c\Õ%™1ù','PO-PAR-2026-0007',_binary '\"$!uC+§“Å\ÌE˙§Ø','parmalat Zambia Ltd','Divine Kabanda','divine@parmalatzambia.com','+260976515732','2026-06-26',105.00,20.00,2100.00,'2026-06-26','lusaka warehouse plot 222000',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-06-26 11:41:05',NULL,NULL,NULL,NULL,NULL,'2026-06-26 11:39:28','2026-06-26 11:41:05','2026-06-26 11:39:44'),(_binary '\‹\—\’\◊L$Ü[\ ¡\Îe','PO-PAR-2026-0012',_binary 'ºÉ¡∞yÛJiò7ˆ\Ê\«`¢S','parmalat Zambia Ltd','Divine Kabanda','divine@parmalatzambia.com','+260976515732','2026-09-16',130.00,25.00,3250.00,'2026-09-18','Chamba Valley, Plot No.2311, Lusaka Zambia',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-09-16 01:23:34',NULL,NULL,NULL,NULL,NULL,'2026-09-16 01:23:01','2026-09-16 01:23:34','2026-09-16 01:23:19'),(_binary '=∂\Àk\ƒqH†èW\Ë\ÎûFÒY','PO-PAR-2026-0011',_binary 'mí¨1\÷M,∂()o∞ﬂ≠','parmalat Zambia Ltd','Divine Kabanda','divine@parmalatzambia.com','+260976515732','2026-09-16',100.00,25.00,2500.00,'2026-07-09','lusaka warehouse',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-09-16 01:15:23',NULL,NULL,NULL,NULL,NULL,'2026-09-16 01:14:47','2026-09-16 01:15:23','2026-09-16 01:15:06'),(_binary 'C(AãçDBìñA+\·™ô','PO-PAR-2026-0008',_binary '≈Æ\Ï\0Dú>A§˘˙0i','parmalat Zambia Ltd','Divine Kabanda','divine@parmalatzambia.com','+260976515732','2026-06-26',150.00,20.00,3000.00,'2026-06-23','lusaka warehouse plot 222000',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-06-28 23:27:41',NULL,NULL,NULL,NULL,NULL,'2026-06-26 11:40:03','2026-06-28 23:27:41','2026-06-26 11:40:07'),(_binary 'RiWQ≤Fò¶Ö)Øu','PO-PRO-2026-0004',_binary '\–\ZLfcäNuπ&X\nŸüë','Processing limited PLC zambia','Silanda J. Mofu','processinglimited@gmail.com','0769312785','2026-09-11',100.00,25.00,2500.00,'2026-09-14','lusaka warehouse plot 222000',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-09-11 09:57:44',NULL,NULL,NULL,NULL,NULL,'2026-09-11 09:54:34','2026-09-11 09:57:44','2026-09-11 09:55:04'),(_binary 'a\ÌıˇØ(O…´J¡ˇ8ß>Ω','PO-PAR-2026-0006',_binary 'î˛•booH¢©ß£=Æ¯\œ\Í','parmalat Zambia Ltd','Divine Kabanda','divine@parmalatzambia.com','+260976515732','2026-06-22',100.00,20.00,2000.00,'2026-06-25','lusaka warehouse plot 222000',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-06-22 20:03:52',NULL,NULL,NULL,NULL,NULL,'2026-06-22 19:59:54','2026-06-22 20:03:52','2026-06-22 20:00:15'),(_binary 'e\ÃGå£\ H»Øπ®ıˆ8','PO-PAR-2026-0009',_binary '9õ;\Ô\›U@èµCΩ\¬1∏ø','parmalat Zambia Ltd','Divine Kabanda','divine@parmalatzambia.com','+260976515732','2026-06-29',101.00,20.00,2020.00,'2026-07-02','lusaka warehouse plot 222000',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-07-01 18:10:46',NULL,NULL,NULL,NULL,NULL,'2026-06-28 23:25:58','2026-07-01 18:10:46','2026-06-28 23:26:07'),(_binary '|*q¨N¨ÇQ\È,p5KR','PO-PRO-2026-0002',_binary '¨3\‘\‚nFıíXN{±±‘Ω','Processing limited PLC zambia','Silanda J. Mofu','processinglimited@gmail.com','0769312785','2026-07-06',105.00,20.00,2100.00,'2026-07-09','Lusaka industrial area',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-07-06 13:33:58',NULL,NULL,NULL,NULL,NULL,'2026-07-06 13:30:38','2026-07-06 13:33:58','2026-07-06 13:30:57'),(_binary 'Ç∂æéEY¶óª˛öñ','PO-MIL-2026-0001',_binary '˝\ZÑAIÃÑ\«¯µ:\ﬂl7','Milk Productions Zambia','Siwale PW','milkproductions@gmail.com','0979988876','2026-07-01',150.00,20.00,3000.00,'2026-06-28','',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-07-01 13:13:21',NULL,NULL,NULL,NULL,NULL,'2026-07-01 12:49:14','2026-07-01 13:13:21','2026-07-01 12:49:42'),(_binary 'ålk®\Ÿ5@¢Ñ1ô\∆\¬\…/\ ','PO-MIL-2026-0003',_binary '°ÒG\‡\ÈJKä\ﬁC%j5','Milk Productions Zambia','Siwale PW','milkproductions@gmail.com','0979988876','2026-09-16',90.00,20.00,1800.00,'2026-07-03','Kitwe 00099',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-09-16 09:32:46',NULL,NULL,NULL,NULL,NULL,'2026-09-16 08:50:38','2026-09-16 09:32:46','2026-09-16 08:50:54'),(_binary 'è\Â\·É1\ÓC\0á\ÿ?\0b\”\›','PO-PAR-2026-0010',_binary '/yã7ÑHÙ™\Àªï\Ôf','parmalat Zambia Ltd','Divine Kabanda','divine@parmalatzambia.com','+260976515732','2026-09-15',150.00,25.00,3750.00,'2026-09-17','lusaka warehouse plot 222000',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-09-15 20:42:47',NULL,NULL,NULL,NULL,NULL,'2026-09-15 20:41:09','2026-09-15 20:42:47','2026-09-15 20:41:17'),(_binary 'í\Íî>∑EJ±ê\ÂÄV\Î¨(','PO-PAR-2026-0003',_binary '\‰hÄ\Â+†I≥±`\·õW!\Î5','parmalat Zambia Ltd','Divine Kabanda','divine@parmalatzambia.com','+260976515732','2026-05-17',78.00,20.00,1560.00,'2026-05-20','plot no 789 lusaka',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-05-17 13:19:57',NULL,NULL,NULL,NULL,NULL,'2026-05-17 13:18:41','2026-05-17 13:19:57','2026-05-17 13:18:41'),(_binary 'ûÚu\rmÄI£æg|ãó\„¨','PO-PAR-2026-0002',_binary 'ª á7ºBFóá°ùÕªSÿí','parmalat Zambia Ltd','Divine Kabanda','divine@parmalatzambia.com','+260976515732','2026-05-17',102.00,20.01,2041.02,'2026-05-20','plot No.90900, lusaka industrial area ',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-05-17 11:42:27',NULL,NULL,NULL,NULL,NULL,'2026-05-16 22:36:13','2026-05-17 11:42:27','2026-05-16 22:36:13'),(_binary 'πà\‡\\®˝J◊è≥\ Ïü∑\∆','PO-PRO-2026-0005',_binary 'â˛	\›5O¨àÉ\'2öS’á','Processing limited PLC zambia','Silanda J. Mofu','processinglimited@gmail.com','0769312785','2026-09-11',100.00,25.00,2500.00,'2026-07-09','lusaka warehouse',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-09-11 10:33:17',NULL,NULL,NULL,NULL,NULL,'2026-09-11 10:31:22','2026-09-11 10:33:17','2026-09-11 10:31:34'),(_binary 'æ® \‰á\◊N,∑\Õe]É˚\Õ','PO-MOR-2026-0001',_binary 'Û\ÀF™ÛLKì—Üpùd\Œr','MORDEN','GERSHION CHOMBA','gershomchomba@gmail.com','0767077387','2026-09-11',104.84,25.00,2621.00,'2026-09-14','lusaka warehouse',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-09-11 12:29:06',NULL,NULL,NULL,NULL,NULL,'2026-09-11 12:09:05','2026-09-11 12:29:06','2026-09-11 12:09:13'),(_binary 'ƒ±=R\ÍEä¢lV8\‡úq','PO-PRO-2026-0006',_binary '\‚Ö^ˇ!\ﬂHÇø\√Ò(¿≤e','Processing limited PLC zambia','Silanda J. Mofu','processinglimited@gmail.com','0769312785','2026-09-11',110.00,25.00,2750.00,'2026-09-14','Chamba Valley, Plot No.2311, Lusaka Zambia',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-09-16 00:32:22',NULL,NULL,NULL,NULL,NULL,'2026-09-11 11:57:23','2026-09-16 00:32:22','2026-09-11 11:57:34'),(_binary '\›t¥\”C˘®ÿûÄñ _Q','PO-PRO-2026-0001',_binary 'ÜéRO¬ì∫§m˛A\«~','Processing limited PLC zambia','Silanda J. Mofu','processinglimited@gmail.com','0769312785','2026-06-28',120.00,20.00,2400.00,'2026-06-25','Chamba Valley, Plot No.2311, Lusaka Zambia',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-06-28 22:52:50',NULL,NULL,NULL,NULL,NULL,'2026-06-28 21:15:06','2026-06-28 22:52:50','2026-06-28 21:18:22'),(_binary '\‚>øTá8B™°4Y|œ™!˙','PO-MIL-2026-0002',_binary '\n¡À≠Û\…EÛéô\“P\≈&£','Milk Productions Zambia','Siwale PW','milkproductions@gmail.com','0979988876','2026-07-01',80.00,20.00,1600.00,'2026-07-05','Lusaka city ',NULL,'Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-07-07 14:55:34',NULL,NULL,NULL,NULL,NULL,'2026-07-01 18:30:58','2026-07-07 14:55:34','2026-07-01 18:31:10'),(_binary '\Íß\ÿ\œ\‡|J§à.Uù¿,','PO-PAR-2026-0001',_binary '\Â)e\…¡_N¨æ\⁄ˆç™uK','parmalat Zambia Ltd','Divine Kabanda','divine@parmalatzambia.com','+260976515732','2026-05-15',100.00,20.00,2000.00,'2026-05-20','31629, LUSAKA INDUSTRIAL AREA','','Net 30 days','APPROVED',_binary '\ËﬁöUòC÷∂Û)\Ê]Dú$','2026-05-15 09:44:51',NULL,NULL,NULL,NULL,NULL,'2026-05-15 09:41:45','2026-05-15 09:44:51','2026-05-15 09:41:45');
/*!40000 ALTER TABLE `purchase_orders` ENABLE KEYS */;
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
