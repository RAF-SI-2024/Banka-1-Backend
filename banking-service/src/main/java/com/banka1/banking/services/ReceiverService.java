package com.banka1.banking.services;

import com.banka1.banking.dto.ReceiverDTO;
import com.banka1.banking.models.Receiver;
import com.banka1.banking.repository.ReceiverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReceiverService {

    private final ReceiverRepository receiverRepository;

    public Receiver findById(Long receiverId) {
        return receiverRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Primalac sa ID-jem " + receiverId + " nije pronađen"));
    }

    public Receiver createReceiver(ReceiverDTO receiverDTO){

        if (receiverRepository.existsByOwnerAccountIdAndAccountNumber(
                receiverDTO.getOwnerAccountId(), receiverDTO.getAccountNumber())) {
            throw new IllegalArgumentException("Primalac sa ovim brojem računa već postoji za datog korisnika.");
        }

        Receiver receiver = new Receiver();

        receiver.setOwnerAccountId(receiverDTO.getOwnerAccountId());
        receiver.setAccountNumber(receiverDTO.getAccountNumber());

        String[] fullName = receiverDTO.getFullName().trim().split("\\s+",2);
        receiver.setFirstName(fullName[0]);
        receiver.setLastName(fullName.length > 1 ? fullName[1] : "");

        receiver.setAddress(receiverDTO.getAddress());

        return receiverRepository.save(receiver);
    }

    public List<Receiver> getReceiversByAccountId(Long accountId){
        return receiverRepository.findByOwnerAccountId(accountId);
    }

    public Receiver updateReceiver(Long id,ReceiverDTO receiverDTO){
        Optional<Receiver> optionalReceiver = receiverRepository.findById(id);

        if(optionalReceiver.isEmpty()){
            throw new IllegalArgumentException("Primalac sa ID " + id + " ne postoji.");
        }

        Receiver receiver = optionalReceiver.get();

        receiver.setAccountNumber(receiverDTO.getAccountNumber());
        if(receiverDTO.getFullName() != null){
            String[] fullName = receiverDTO.getFullName().trim().split("\\s+",2);
            receiver.setFirstName(fullName[0]);
            receiver.setLastName(fullName.length > 1 ? fullName[1] : "");
        }

        receiver.setAddress(receiverDTO.getAddress());

        return receiverRepository.save(receiver);
    }

    public void deleteReceiver(Long id) {
        if (!receiverRepository.existsById(id)) {
            throw new IllegalArgumentException("Primalac sa ID " + id + " ne postoji.");
        }
        receiverRepository.deleteById(id);
    }

    public boolean accountExists(Long id){
        return receiverRepository.existsById(id);
    }

}
