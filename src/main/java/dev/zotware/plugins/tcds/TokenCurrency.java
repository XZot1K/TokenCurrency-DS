package dev.zotware.plugins.tcds;

import com.vk2gpz.tokenenchant.api.TokenEnchantAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.enums.EconomyCallType;
import xzot1k.plugins.ds.api.events.AffordCheckEvent;
import xzot1k.plugins.ds.api.events.CurrencyTransferEvent;

import java.util.logging.Level;

public final class TokenCurrency extends JavaPlugin implements Listener {

    private static TokenCurrency instance;
    private DisplayShops displayShops;
    private TokenEnchantAPI tokenEnchantAPI;

    @Override
    public void onEnable() {
        instance = this;

        if (getServer().getPluginManager().getPlugin("DisplayShops") == null) {
            getServer().getLogger().log(Level.WARNING, "DisplayShops was unable to be found. Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        } else displayShops = DisplayShops.getPluginInstance();

        if (getServer().getPluginManager().getPlugin("TokenEnchant") == null) {
            getServer().getLogger().log(Level.WARNING, "TokenEnchant was unable to be found. Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        } else tokenEnchantAPI = TokenEnchantAPI.getInstance();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAfford(AffordCheckEvent e) {
        e.setCancelled(true);
        if (e.getEconomyCallEvent().getEconomyCallType() == EconomyCallType.BUY) {
            e.setCanInvestorAfford(e.getInvestor().hasPermission("displayshops.bypass")
                    || getTokenEnchantAPI().getTokens(e.getInvestor()) >= e.getTaxedPrice());
        } else if (e.getEconomyCallEvent().getEconomyCallType() == EconomyCallType.SELL) {
            if (e.getProducer() != null) e.setCanProducerAfford(getTokenEnchantAPI().getTokens(e.getProducer()) >= e.getPrice()
                    || (e.getShop().isAdminShop() || e.getShop().getStoredBalance() >= e.getPrice()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTransfer(CurrencyTransferEvent e) {
        e.setCancelled(true);
        if (e.getEconomyCallType() == EconomyCallType.SELL) {
            if (e.getInvestor() != null) tokenEnchantAPI.addTokens(e.getInvestor(), e.getPrice());

            if (!e.getShop().isAdminShop()) {
                if (e.getProducer() != null) {
                    tokenEnchantAPI.removeTokens(e.getProducer(), e.getPrice());
                    return;
                }

                final double storedPriceCalculation = (e.getShop().getStoredBalance() - e.getPrice());
                e.getShop().setStoredBalance(Math.max(storedPriceCalculation, 0));
            }
            return;
        }

        if (e.getInvestor() != null && e.shouldChargeInvestor()) {
            tokenEnchantAPI.removeTokens(e.getInvestor(), e.getTaxedPrice());
            e.setInvestorCharged(true);
        }

        if ((e.getEconomyCallType() != EconomyCallType.EDIT_ACTION && e.getEconomyCallType() != EconomyCallType.RENT
                && e.getEconomyCallType() != EconomyCallType.RENT_RENEW) && !e.getShop().isAdminShop()) {
            if (e.getProducer() != null) {
                tokenEnchantAPI.addTokens(e.getProducer(), e.getPrice());
                return;
            }

            e.getShop().setStoredBalance(e.getShop().getStoredBalance() + e.getPrice());
        }
    }
    
    // getters & setters
    public DisplayShops getDisplayShops() {
        return displayShops;
    }

    public TokenEnchantAPI getTokenEnchantAPI() {
        return tokenEnchantAPI;
    }

    public static TokenCurrency getInstance() {
        return instance;
    }
}
