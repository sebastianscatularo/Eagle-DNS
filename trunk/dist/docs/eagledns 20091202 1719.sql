-- MySQL Administrator dump 1.4
--
-- ------------------------------------------------------
-- Server version	5.0.87-community-nt


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;


--
-- Create schema eagledns
--

CREATE DATABASE IF NOT EXISTS eagledns;
USE eagledns;

--
-- Definition of table `records`
--

DROP TABLE IF EXISTS `records`;
CREATE TABLE `records` (
  `recordID` int(10) unsigned NOT NULL auto_increment,
  `zoneID` int(10) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` varchar(6) NOT NULL,
  `content` varchar(255) NOT NULL,
  `ttl` int(10) unsigned default NULL,
  `prio` varchar(45) default NULL,
  `dclass` varchar(6) NOT NULL,
  PRIMARY KEY  (`recordID`),
  KEY `FK_records_1` (`zoneID`),
  CONSTRAINT `FK_records_1` FOREIGN KEY (`zoneID`) REFERENCES `zones` (`zoneID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `records`
--

/*!40000 ALTER TABLE `records` DISABLE KEYS */;
INSERT INTO `records` (`recordID`,`zoneID`,`name`,`type`,`content`,`ttl`,`prio`,`dclass`) VALUES 
 (1,1,'@','NS','dns.unlogic.se.',NULL,NULL,'IN'),
 (2,1,'@','NS','dns2.unlogic.se.',NULL,NULL,'IN'),
 (3,1,'@','MX','ASPMX.L.GOOGLE.COM.',NULL,'10','IN'),
 (4,1,'@','MX','ALT1.ASPMX.L.GOOGLE.COM.',NULL,'20','IN'),
 (5,1,'@','MX','ALT2.ASPMX.L.GOOGLE.COM.',NULL,'20','IN'),
 (6,1,'@','MX','ASPMX2.GOOGLEMAIL.COM.',NULL,'30','IN'),
 (7,1,'@','MX','ASPMX3.GOOGLEMAIL.COM.',NULL,'30','IN'),
 (8,1,'@','MX','ASPMX4.GOOGLEMAIL.COM.',NULL,'30','IN'),
 (9,1,'@','MX','ASPMX5.GOOGLEMAIL.COM. ',NULL,'30','IN'),
 (10,1,'@','A','85.230.75.184',NULL,NULL,'IN'),
 (11,1,'dns','A','85.230.75.184',NULL,NULL,'IN'),
 (12,1,'dns2','A','85.224.248.126',NULL,NULL,'IN'),
 (13,1,'mail','A','85.230.75.184',NULL,NULL,'IN'),
 (14,1,'www','A','85.230.75.184',NULL,NULL,'IN'),
 (15,1,'ftp','A','85.230.75.184',NULL,NULL,'IN'),
 (16,1,'pics','A','85.230.75.184',NULL,NULL,'IN'),
 (17,1,'stats','A','85.230.75.184',NULL,NULL,'IN'),
 (18,1,'mimer','A','85.230.75.184',NULL,NULL,'IN'),
 (19,1,'@','TXT','\"v=spf1 mx a:smtp.bredband.net include:aspmx.googlemail.com ~all\"',NULL,NULL,'IN');
/*!40000 ALTER TABLE `records` ENABLE KEYS */;


--
-- Definition of table `zones`
--

DROP TABLE IF EXISTS `zones`;
CREATE TABLE `zones` (
  `zoneID` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(255) NOT NULL,
  `dclass` varchar(6) NOT NULL,
  `primaryDNS` varchar(255) NOT NULL,
  `adminEmail` varchar(255) default NULL,
  `serial` int(10) unsigned default NULL,
  `refresh` int(10) unsigned default NULL,
  `retry` int(10) unsigned default NULL,
  `expire` int(10) unsigned default NULL,
  `minimum` int(10) unsigned default NULL,
  `secondary` tinyint(1) NOT NULL,
  `ttl` int(10) unsigned default NULL,
  PRIMARY KEY  (`zoneID`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `zones`
--

/*!40000 ALTER TABLE `zones` DISABLE KEYS */;
INSERT INTO `zones` (`zoneID`,`name`,`dclass`,`primaryDNS`,`adminEmail`,`serial`,`refresh`,`retry`,`expire`,`minimum`,`secondary`,`ttl`) VALUES 
 (1,'unlogic.se.','IN','dns.unlogic.se.','unlogic.unlogic.se.',20091118,300,120,86400,300,0,300);
/*!40000 ALTER TABLE `zones` ENABLE KEYS */;




/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
