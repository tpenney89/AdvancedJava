package tpenney.services;

import tpenney.model.User;
import tpenney.model.database.PersonDAO;
import tpenney.model.database.PersonStocksDAO;
import tpenney.model.database.StockSymbolDAO;
import tpenney.util.DatabaseUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;

/**
 * An implementation of the UserService interface that gets uses a database.
 */
class DatabaseUserService implements UserService {

    /**
     * Add a user to the system.
     *
     * @param user the person to add.
     * @throws UserServiceException       if there is a problem creating the person record.
     * @throws DuplicateUserNameException if the user name is not unique.
     */
    @Override
    public void addPerson(User user) throws UserServiceException, DuplicateUserNameException {
        Transaction transaction = null;
        Session session = null;
        try {
            session = DatabaseUtils.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            PersonDAO personDAO = new PersonDAO();
            personDAO.setUserName(user.getUserName());
            session.saveOrUpdate(personDAO);
            transaction.commit();
        } catch (ConstraintViolationException e) {
            throw new DuplicateUserNameException(user.getUserName() + " already exists");
        } catch (HibernateException e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();  // close transaction
            }
            throw new UserServiceException(e.getMessage(), e);
        } finally {
            if (transaction != null && transaction.isActive()) {
                // if we get there there's an error to deal with
                transaction.rollback();  //  close transaction
            }
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * Each person can have 0 or more stocks associated with them. This methods adds that association.
     *
     * @param symbol the symbol to add
     * @param user   the user name to add the symbol to
     * @throws UnknownStockSymbolException if the stock symbol does not exist in the supported list
     *                                     of symbols.
     * @throws UnknownUserException        if the specified user can't be found.
     * @throws UserServiceException        if there was a general problem with the service.
     */
    @Override
    public void associateStockWithPerson(String symbol, User user)
            throws UnknownStockSymbolException, UnknownUserException, UserServiceException {

        PersonDAO personDAO = DatabaseUtils.findUniqueResultBy("username", user.getUserName(), PersonDAO.class, true);
        if (personDAO == null) {
            throw new UnknownUserException("No Person record found with username of " + user.getUserName());
        }
        StockSymbolDAO stockSymbolDAO = DatabaseUtils.findUniqueResultBy("symbol", symbol, StockSymbolDAO.class, true);
        if (stockSymbolDAO == null) {
            throw new UnknownStockSymbolException("No Stock Symbol record for: " + symbol);
        }
        PersonStocksDAO personStocksDAO = new PersonStocksDAO();
        personStocksDAO.setPersonDAO(personDAO);
        //  personStocksDAO.setPersonByPersonId(stockSymbolDAO);
        Session session = DatabaseUtils.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            session.saveOrUpdate(personStocksDAO);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();  // close transaction
            }
        } finally {
            if (transaction != null && transaction.isActive()) {
                transaction.commit();
            }
        }


    }
}
