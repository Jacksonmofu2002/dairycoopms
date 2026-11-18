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
-- Table structure for table `staff_salaries`
--

DROP TABLE IF EXISTS `staff_salaries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `staff_salaries` (
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `period_year` int NOT NULL,
  `period_month` int NOT NULL,
  `basic_salary` decimal(14,2) NOT NULL,
  `deductions` decimal(14,2) NOT NULL DEFAULT '0.00',
  `net_salary` decimal(14,2) NOT NULL,
  `payment_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'UNPAID',
  `paid_at` timestamp NULL DEFAULT NULL,
  `payment_method` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reference_no` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notes` text COLLATE utf8mb4_unicode_ci,
  `processed_by` binary(16) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `salary_notification_read` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_staff_salary_period` (`user_id`,`period_year`,`period_month`),
  KEY `idx_staff_salary_user` (`user_id`),
  KEY `idx_staff_salary_period` (`period_year`,`period_month`),
  KEY `idx_staff_salary_status` (`payment_status`),
  KEY `fk_staff_salary_processed_by` (`processed_by`),
  CONSTRAINT `fk_staff_salary_processed_by` FOREIGN KEY (`processed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_staff_salary_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `staff_salaries`
--

LOCK TABLES `staff_salaries` WRITE;
/*!40000 ALTER TABLE `staff_salaries` DISABLE KEYS */;
INSERT INTO `staff_salaries` VALUES (_binary '\ß\Ùû¥ûBY‘¨˜=a',_binary '\Ø%·)B.Gl¿ò]åŸB7',2026,6,7000.00,0.00,7000.00,'PAID','2026-07-06 14:02:36','BANK_TRANSFER','SAL-2026-0016','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-06-09 10:44:54','2026-07-06 14:02:35',_binary '\0'),(_binary 'r8¡wNAû\ZÁL€',_binary '\Ïº©fK\íy0q\åş€R',2026,3,7000.00,0.00,7000.00,'PAID','2026-06-09 12:41:36','BANK_TRANSFER','SAL-2026-0011','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-05-13 00:40:48','2026-06-09 12:41:36',_binary '\0'),(_binary '²@.Sõ@«—C9÷V\Ú7',_binary '\èŞšğU˜CÖ¶ó)\æ]Dœ$',2026,6,7000.00,0.00,7000.00,'PAID','2026-07-06 14:02:11','BANK_TRANSFER','SAL-2026-0015','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-06-09 10:44:54','2026-07-06 14:02:11',_binary '\0'),(_binary '&¨p+!L®\\\ì^Œ\Í\nR',_binary '\Ïº©fK\íy0q\åş€R',2026,9,7000.00,0.00,7000.00,'PAID','2026-09-11 12:50:23','BANK_TRANSFER','SAL-2026-0022','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-09-11 12:46:32','2026-09-11 12:50:22',_binary '\0'),(_binary '+FHSA\'M^\î\ß',_binary '\Ø%·)B.Gl¿ò]åŸB7',2026,4,7000.00,0.00,7000.00,'PAID','2026-05-11 14:02:13','BANK_TRANSFER','SAL-2026-0001','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-05-04 14:40:33','2026-05-11 14:02:13',_binary '\0'),(_binary '.¬\áùRJSŸ€\×1‚',_binary '\Ø%·)B.Gl¿ò]åŸB7',2026,5,7000.00,0.00,7000.00,'PAID','2026-05-19 15:54:56','BANK_TRANSFER','SAL-2026-0005','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-05-13 00:18:14','2026-05-19 15:54:56',_binary '\0'),(_binary '1˜ºk3C¤—˜òH)6',_binary '\èŞšğU˜CÖ¶ó)\æ]Dœ$',2026,7,7000.00,0.00,7000.00,'PAID','2026-09-11 12:48:11','BANK_TRANSFER','SAL-2026-0018','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-07-06 14:03:30','2026-09-11 12:48:10',_binary '\0'),(_binary '8)j@«Ml®\×qhÏ¢\Ã',_binary '\Ïº©fK\íy0q\åş€R',2026,6,7000.00,0.00,7000.00,'PAID','2026-06-09 12:38:00','BANK_TRANSFER','SAL-2026-0010','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-06-09 10:44:54','2026-06-09 12:38:00',_binary '\0'),(_binary ':ß±–F)Lo%\Ô\È',_binary '\Ø%·)B.Gl¿ò]åŸB7',2026,9,7000.00,0.00,7000.00,'UNPAID',NULL,NULL,NULL,NULL,NULL,'2026-09-11 12:46:32','2026-09-11 12:46:32',_binary ''),(_binary '>+ø˜gLÉ¯zZÓŸ·\ß9',_binary '\Ø%·)B.Gl¿ò]åŸB7',2026,3,7000.00,0.00,7000.00,'PAID','2026-07-02 19:19:20','BANK_TRANSFER','SAL-2026-0013','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-05-13 00:40:48','2026-07-02 19:19:19',_binary '\0'),(_binary 'GA\ÏD\İ]B—Ÿ#–š\Z\Ó<',_binary '\èŞšğU˜CÖ¶ó)\æ]Dœ$',2026,5,7000.00,0.00,7000.00,'PAID','2026-05-19 15:59:25','BANK_TRANSFER','SAL-2026-0006','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-05-13 00:18:14','2026-05-19 15:59:25',_binary '\0'),(_binary 'X²\Z\ìúMxƒµ3‰\×ñ÷',_binary '\èŞšğU˜CÖ¶ó)\æ]Dœ$',2026,9,7000.00,0.00,7000.00,'UNPAID',NULL,NULL,NULL,NULL,NULL,'2026-09-11 12:46:32','2026-09-11 12:46:32',_binary ''),(_binary '_:<\Êw‰FXª\ê:ÿ,nó',_binary '¨Ge±F\îñ®\Ä\è\ØÑŸH',2026,5,8000.00,0.00,8000.00,'PAID','2026-05-19 16:19:35','BANK_TRANSFER','SAL-2026-0007','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-05-13 00:18:14','2026-05-19 16:19:35',_binary '\0'),(_binary 'aû\Z!˜²H£¸N|2~™',_binary '¨Ge±F\îñ®\Ä\è\ØÑŸH',2026,4,8000.00,0.00,8000.00,'PAID','2026-05-11 14:02:27','BANK_TRANSFER','SAL-2026-0002','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-05-04 14:40:33','2026-05-11 14:02:26',_binary '\0'),(_binary 'w\0\ã\Ìü\×Oj¶hA™¦*T\Ë',_binary '\Ïº©fK\íy0q\åş€R',2026,5,7000.00,0.00,7000.00,'PAID','2026-05-19 16:39:20','BANK_TRANSFER','SAL-2026-0008','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-05-13 00:18:14','2026-05-19 16:39:20',_binary '\0'),(_binary '·\Ï\ë{ºG½“ó\Ğ[‘\"',_binary '\Ïº©fK\íy0q\åş€R',2026,7,7000.00,0.00,7000.00,'PAID','2026-09-11 12:47:45','BANK_TRANSFER','SAL-2026-0017','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-07-06 14:03:30','2026-09-11 12:47:44',_binary '\0'),(_binary '°%H@òKd¾\ÅU©&,',_binary '\Ø%·)B.Gl¿ò]åŸB7',2026,7,7000.00,0.00,7000.00,'PAID','2026-09-11 12:48:33','BANK_TRANSFER','SAL-2026-0019','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-07-06 14:03:30','2026-09-11 12:48:33',_binary '\0'),(_binary '–ı,Ÿ^\ØOo‚+E-¡rüŠ',_binary '¨Ge±F\îñ®\Ä\è\ØÑŸH',2026,9,8000.00,0.00,8000.00,'PAID','2026-09-11 12:49:25','BANK_TRANSFER','SAL-2026-0021','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-09-11 12:46:32','2026-09-11 12:49:24',_binary '\0'),(_binary '£ˆ_\Æ\Z·Cåº»^ş|§	',_binary '\èŞšğU˜CÖ¶ó)\æ]Dœ$',2026,4,7000.00,0.00,7000.00,'PAID','2026-05-11 14:02:51','BANK_TRANSFER','SAL-2026-0004','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-05-04 14:40:33','2026-05-11 14:02:51',_binary '\0'),(_binary '«‡CD-CP€\Ò4	´£4',_binary '\èŞšğU˜CÖ¶ó)\æ]Dœ$',2026,3,7000.00,0.00,7000.00,'PAID','2026-07-02 19:19:48','BANK_TRANSFER','SAL-2026-0014','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-05-13 00:40:48','2026-07-02 19:19:48',_binary '\0'),(_binary 'ÃŸğÿ\ÄF¿pƒ–m\rS¯',_binary '¨Ge±F\îñ®\Ä\è\ØÑŸH',2026,6,8000.00,0.00,8000.00,'PAID','2026-06-09 12:41:55','BANK_TRANSFER','SAL-2026-0012','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-06-09 10:44:54','2026-06-09 12:41:54',_binary '\0'),(_binary 'É¼¸\Ñ[JÊ¬0*I¯54¾',_binary '¨Ge±F\îñ®\Ä\è\ØÑŸH',2026,3,8000.00,0.00,8000.00,'PAID','2026-06-09 10:38:53','BANK_TRANSFER','SAL-2026-0009','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-05-13 00:40:48','2026-06-09 10:38:53',_binary '\0'),(_binary 'Ş¢±p\rD¸ô\È+¿ C°',_binary '\Ïº©fK\íy0q\åş€R',2026,4,7000.00,0.00,7000.00,'PAID','2026-05-11 14:02:39','BANK_TRANSFER','SAL-2026-0003','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-05-04 14:40:33','2026-05-11 14:02:38',_binary '\0'),(_binary '\ãe8ĞŒ\ŞA\á²ÛŒ\ï\ï7',_binary '¨Ge±F\îñ®\Ä\è\ØÑŸH',2026,7,8000.00,0.00,8000.00,'PAID','2026-09-11 12:49:02','BANK_TRANSFER','SAL-2026-0020','',_binary '\Ø%·)B.Gl¿ò]åŸB7','2026-07-06 14:03:30','2026-09-11 12:49:02',_binary '\0');
/*!40000 ALTER TABLE `staff_salaries` ENABLE KEYS */;
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
