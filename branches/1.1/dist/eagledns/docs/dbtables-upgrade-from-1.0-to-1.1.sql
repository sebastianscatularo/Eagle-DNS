CREATE TABLE  `zonealiases` (
  `zoneID` int(10) unsigned NOT NULL auto_increment,
  `alias` varchar(255) NOT NULL,
  PRIMARY KEY  (`zoneID`,`alias`),
  CONSTRAINT `FK_zonealiases_1` FOREIGN KEY (`zoneID`) REFERENCES `zones` (`zoneID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

ALTER TABLE `zones` ADD COLUMN `autoGenerateSerial` BOOLEAN NOT NULL AFTER `downloaded`;

ALTER TABLE `zones` ADD COLUMN `enabled` BOOLEAN NOT NULL AFTER `autoGenerateSerial`;

UPDATE zones SET enabled = true;
