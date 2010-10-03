CREATE TABLE  `zones` (
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
  `downloaded` timestamp NULL default NULL,
  PRIMARY KEY  (`zoneID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;

CREATE TABLE  `zonealiases` (
  `zoneID` int(10) unsigned NOT NULL auto_increment,
  `alias` varchar(255) NOT NULL,
  PRIMARY KEY  (`zoneID`,`alias`),
  CONSTRAINT `FK_zonealiases_1` FOREIGN KEY (`zoneID`) REFERENCES `zones` (`zoneID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

ALTER TABLE `zones` ADD COLUMN `autoGenerateSerial` BOOLEAN NOT NULL AFTER `downloaded`;

CREATE TABLE  `records` (
  `recordID` int(10) unsigned NOT NULL auto_increment,
  `zoneID` int(10) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` varchar(6) NOT NULL,
  `content` varchar(255) NOT NULL,
  `ttl` int(10) unsigned default NULL,
  `dclass` varchar(6) NOT NULL,
  PRIMARY KEY  (`recordID`),
  KEY `FK_records_1` (`zoneID`),
  CONSTRAINT `FK_records_1` FOREIGN KEY (`zoneID`) REFERENCES `zones` (`zoneID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;