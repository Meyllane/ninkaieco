package com.github.meyllane.ninkaiEco.dataclass;

    import com.github.meyllane.ninkaiEco.NinkaiEco;
    import com.github.meyllane.ninkaiEco.enums.BankOperationType;
    import me.Seisan.plugin.Main;

    import java.sql.PreparedStatement;
    import java.sql.SQLException;
    import java.util.Date;
    import java.util.logging.Level;

    /**
     * Represents a bank operation between two players.
     */
    public class BankOperation {
        private final String emitter_UUID;
        private final String receiver_UUID;
        private final int amount;
        private final Date date;
        private final BankOperationType type;

        /**
         * Constructs a new BankOperation with the current date.
         *
         * @param emitter_UUID  the UUID of the player initiating the operation
         * @param receiver_UUID the UUID of the player receiving the operation
         * @param amount        the amount of money involved in the operation
         * @param type          the type of the bank operation
         */
        public BankOperation(String emitter_UUID, String receiver_UUID, int amount, BankOperationType type) {
            this.emitter_UUID = emitter_UUID;
            this.receiver_UUID = receiver_UUID;
            this.amount = amount;
            this.type = type;
            this.date = new Date();
        }

        /**
         * Constructs a new BankOperation with a specified date.
         *
         * @param emitter_UUID  the UUID of the player initiating the operation
         * @param receiver_UUID the UUID of the player receiving the operation
         * @param amount        the amount of money involved in the operation
         * @param type          the type of the bank operation
         * @param date          the date of the operation
         */
        public BankOperation(String emitter_UUID, String receiver_UUID, int amount, BankOperationType type, Date date) {
            this.emitter_UUID = emitter_UUID;
            this.receiver_UUID = receiver_UUID;
            this.amount = amount;
            this.type = type;
            this.date = date;
        }

        /**
         * Saves the bank operation to the database.
         * <p><b>Note: </b>As this operation involves a database connection, it should be run asynchronously (with <code>BukkitScheduler</code>)</p>
         */
        public void flush() {
            try {
                PreparedStatement pst = Main.dbManager.getConnection().prepareStatement(
                        """
                                INSERT INTO BankOperation(emitter_UUID, receiver_UUID, amount, type, date)
                                VALUES (?, ?, ?, ?, ?)
                                """
                );
                pst.setString(1, this.emitter_UUID);
                pst.setString(2, this.receiver_UUID);
                pst.setInt(3, this.amount);
                pst.setString(4, this.type.type);
                pst.setTimestamp(5, new java.sql.Timestamp(this.date.getTime()));
                pst.execute();
                pst.close();
            } catch (SQLException e) {
                NinkaiEco.getPlugin(NinkaiEco.class).getLogger().log(Level.SEVERE,
                        "In BankOperation.flush() : " + e.getMessage());
            }
        }
    }